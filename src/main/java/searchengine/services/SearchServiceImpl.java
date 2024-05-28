package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.QueryTypeMismatchException;
import org.springframework.stereotype.Service;
import searchengine.dto.Datas;
import searchengine.dto.SearchResponse;
import searchengine.models.Lemma;
import searchengine.models.Page;
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
            int maxLemmaPageCount = 100;
            double maxRelevance = 0.0;
            WordLemmatizer wordLemmatizer = WordLemmatizer.getInstance();

            Map<String, Double> pageRelevance = new HashMap<>();
            List<Datas> dataList = new ArrayList<>();
            Map<String, Integer> lemmaMap = new HashMap<>();
            Set<String> lemmas = wordLemmatizer.getLemmaSet(query);

            for (String lemma : lemmas) {
                Lemma lemmaFind = lemmaRepository.findByLemma(lemma);
                int count = lemmaFind.getFrequency();
                lemmaMap.put(lemma, count);
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

            Set<String> resultPages = new HashSet<>();
            for (Map.Entry<String, Integer> lemmaEntry : filteredLemmaMap.entrySet()) {
                String lemma = lemmaEntry.getKey();
                Integer lemmaId = lemmaRepository.findLemmaIdByLemma(lemma);
                if (lemmaId != null) {
                    Set<Page> pagesForLemma = indexedRepository.findPageByLemmaId(lemmaId);

                    Set<String> pathPage = pagesForLemma.stream()
                            .map(Page::getPath)
                            .collect(Collectors.toSet());

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
                    Integer lemmaId = lemmaRepository.findLemmaIdByLemma(lemma);
                    Page page = pageRepository.findByPath(pages).orElseThrow();

                    float ranking = indexedRepository.findRankingByLemmaIdAndPage(lemmaId, page);
                    pageRelevanceSum += ranking;
                }
                double normalizedRelevance = pageRelevanceSum / maxRelevance;
                pageRelevance.put(pages, normalizedRelevance);
                maxRelevance = Math.max(maxRelevance, pageRelevanceSum);
            }

            for (String pages : resultPages) {
                Page page = pageRepository.findByPath(pages).orElseThrow();
                String title = "";
                String snippet = findSnippet(page, query);
                Double relevance = pageRelevance.get(pages);
                String siteName = siteRepository.findNameByUrl(site);

                Datas data = new Datas();
                data.setSite(site);
                data.setSiteName(siteName);
                data.setUri(pages.replaceAll(site, ""));
                data.setTitle(title);
                data.setSnippet(snippet);
                data.setRelevance(relevance);
                dataList.add(data);
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
            String pageContent = pageRepository.findByContent(page);
            String[] sentences = pageContent.split("[.!?]+");
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
}
