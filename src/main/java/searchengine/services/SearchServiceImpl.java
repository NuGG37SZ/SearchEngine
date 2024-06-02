package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.QueryTypeMismatchException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.dto.Datas;
import searchengine.dto.SearchResponse;
import searchengine.models.Indexed;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.Sites;
import searchengine.repository.IndexedRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final IndexedRepository indexedRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public SearchServiceImpl(LemmaRepository lemmaRepository, IndexedRepository indexedRepository,
                             PageRepository pageRepository, SiteRepository siteRepository)
    {
        this.lemmaRepository = lemmaRepository;
        this.indexedRepository = indexedRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        try {
            int maxLemmaPageCount = 1000;
            double maxRelevance = 0.0;
            WordLemmatizer wordLemmatizer = WordLemmatizer.getInstance();

            Map<String, Double> pageRelevance = new HashMap<>();
            List<Datas> dataList = new ArrayList<>();
            Map<String, Integer> lemmaMap = new HashMap<>();
            Set<String> lemmas = wordLemmatizer.getLemmaSet(query);
            Set<String> resultPages = new HashSet<>();
            Set<String> pathPage;

            for (String lemma : lemmas) {
                try {
                    Lemma lemmaFind = lemmaRepository.findByLemma(lemma);
                    int countLemma = lemmaFind.getFrequency();
                    lemmaMap.put(lemma, countLemma);
                } catch (NullPointerException ex) {
                    ex.getMessage();
                }
            }

            Map<String, Integer> filteredLemmaMap = lemmaMap.entrySet().stream()
                    .filter(entry -> entry.getValue() <= maxLemmaPageCount)
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue,
                            LinkedHashMap::new
                    ));

            for (Map.Entry<String, Integer> lemmaEntry : filteredLemmaMap.entrySet()) {
                String lemma = lemmaEntry.getKey();
                Lemma lemmaFind = lemmaRepository.findByLemma(lemma);
                if (lemmaFind.getId() != null) {
                    List<Indexed> indexedList = indexedRepository.findByLemmaId(lemmaFind.getId());
                    Set<Integer> pageIds = indexedList.stream()
                            .map(Indexed::getPage)
                            .map(Page::getId)
                            .collect(Collectors.toSet());

                    if(site != null) {
                        pathPage = pageIds.stream()
                                .map(pageId -> pageRepository.findById(pageId).orElseThrow().getPath())
                                .filter(path -> path.startsWith(site))
                                .collect(Collectors.toSet());
                    } else {
                        pathPage = pageIds.stream()
                                .map(pageId -> pageRepository.findById(pageId).orElseThrow().getPath()
                                        .replaceAll("null", ""))
                                .collect(Collectors.toSet());
                    }

                    if (resultPages.isEmpty()) {
                        resultPages.addAll(pathPage);
                    } else {
                        resultPages.retainAll(pathPage);
                    }

                    if (resultPages.isEmpty()) {
                        break;
                    }
                }
            }

            for (String pages : resultPages) {
                double pageRelevanceSum = 0.0;
                for (Map.Entry<String, Integer> lemmaEntry : filteredLemmaMap.entrySet()) {
                    String lemma = lemmaEntry.getKey();
                    Lemma lemmaFind = lemmaRepository.findByLemma(lemma);
                    Page page = pageRepository.findByPath(pages).orElseThrow();

                    List<Indexed> indexedList = indexedRepository.findByLemmaIdAndPageId(lemmaFind.getId(),
                            page.getId());
                    for(Indexed indexed : indexedList) {
                        float ranking = indexed.getRanking();
                        pageRelevanceSum += ranking;
                    }
                }
                double normalizedRelevance = pageRelevanceSum / maxRelevance;
                pageRelevance.put(pages, normalizedRelevance);
                maxRelevance = Math.max(maxRelevance, pageRelevanceSum);
            }

            for (String pages : resultPages) {
                Optional<Page> optionalPage = pageRepository.findByPath(pages);

                if (optionalPage.isPresent()) {
                    Page page = optionalPage.get();
                    String title = findTitle(page);
                    String snippet = findSnippet(page, query);
                    String uri = page.getPath();
                    Double relevance = pageRelevance.get(pages);

                    Datas data = new Datas();
                    data.setSite(site);
                    data.setUri(uri);
                    data.setTitle(title);
                    data.setSnippet(snippet);
                    data.setRelevance(relevance);

                    Optional<Sites> optionalSite = Optional.ofNullable(siteRepository.findByUrl(site));
                    if (optionalSite.isPresent()) {
                        Sites siteEntity = optionalSite.get();
                        data.setSiteName(siteEntity.getName());
                    } else {
                        Sites siteFind = page.getSite();
                        data.setSiteName(siteFind.getName());
                        data.setSite(siteFind.getUrl());
                    }
                    dataList.add(data);
                }
            }
            dataList.sort((d1, d2) -> Double.compare(d2.getRelevance(), d1.getRelevance()));

            return new SearchResponse(true, null, dataList.size(), dataList);
        } catch (IOException | QueryTypeMismatchException ex) {
            ex.printStackTrace();
            return new SearchResponse(false, ex.getMessage(), 0, Collections.emptyList());
        }
    }

    private String findSnippet(Page page, String query) {
        try {
            String content = page.getContent();
            String[] sentences = content.split("[.!?]+");
            List<String> relevantSentences = new ArrayList<>();

            for (String sentence : sentences) {
                if (containsQueryTerms(sentence, query)) {
                    relevantSentences.add(sentence);
                }
            }

            if (!relevantSentences.isEmpty()) {
                StringBuilder snippetBuilder = new StringBuilder();
                for (int i = 0; i < Math.min(3, relevantSentences.size()); i++) {
                    snippetBuilder.append(relevantSentences.get(i)).append(" ");
                }
                return snippetBuilder.toString().trim();
            }

            return "No relevant snippet found for the given page and query.";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error occurred while generating the snippet.";
        }
    }

    private boolean containsQueryTerms(String sentence, String query) {
        String[] queryTerms = query.split(" ");
        for (String term : queryTerms) {
            if (!sentence.toLowerCase().contains(term.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private String findTitle(Page page) {
        Document doc = Jsoup.parse(page.getContent());
        Elements titleElements = doc.select("title");
        String title = titleElements.isEmpty() ? "" : titleElements.first().text();
        return title;
    }
}
