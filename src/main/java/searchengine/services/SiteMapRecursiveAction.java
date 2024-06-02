package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.enums.Status;
import searchengine.models.Indexed;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.Sites;
import searchengine.repository.IndexedRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;

import static java.lang.Thread.sleep;

@Slf4j
public class SiteMapRecursiveAction extends RecursiveAction {
    private final Sites site;
    private final String url;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexedRepository indexedRepository;
    private final ConcurrentSkipListSet<String> processedLinks;

    public SiteMapRecursiveAction(Sites site, String url, PageRepository pageRepository,
                                  SiteRepository siteRepository, ConcurrentSkipListSet<String> processedLinks,
                                  LemmaRepository lemmaRepository, IndexedRepository indexedRepository)
    {
        this.site = site;
        this.url = url;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.processedLinks = processedLinks;
        this.lemmaRepository = lemmaRepository;
        this.indexedRepository = indexedRepository;
    }

    @Override
    protected void compute() {
        Page page;

        if (processedLinks.contains(url)) {
            return;
        }
        processedLinks.add(url);

        try {
            sleep(150);
            Connection connection = Jsoup.connect(url)
                    .ignoreHttpErrors(true)
                    .timeout(5000)
                    .followRedirects(false);
            Document document = connection.get();
            Elements elements = document.select("a[href]");

            Optional<Page> pageOptional = pageRepository.findBySiteAndPath(site, url);
            if (pageOptional.isPresent()) {
                page = pageOptional.get();
                page.setCode(document.connection().response().statusCode());
                page.setContent(document.html());
                pageRepository.save(page);
            } else {
                page = new Page();
                page.setSite(site);
                page.setCode(document.connection().response().statusCode());
                page.setContent(document.html());
                page.setPath(url);
                pageRepository.save(page);
            }

            WordLemmatizer wordLemmatizer = WordLemmatizer.getInstance();
            String cleanHtmlCode = wordLemmatizer.cleanHtmlTags(page.getContent());
            Map<String, Integer> lemmasMap = wordLemmatizer.collectLemmas(cleanHtmlCode);

            for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
                String lemmaStr = entry.getKey();
                int frequency = entry.getValue();
                Lemma lemma = lemmaRepository.findByLemma(lemmaStr);

                if (lemma != null) {
                    lemma.setFrequency(lemma.getFrequency() + 1);
                    lemmaRepository.save(lemma);
                } else {
                    lemma = new Lemma();
                    lemma.setLemma(lemmaStr);
                    lemma.setFrequency(frequency);
                    lemma.setSite(site);
                    lemmaRepository.save(lemma);
                }

                Indexed indexed = new Indexed();
                indexed.setPage(page);
                indexed.setLemma(lemma);
                indexed.setRanking(frequency);
                indexedRepository.save(indexed);
            }

            List<SiteMapRecursiveAction> taskList = new ArrayList<>();
            for (Element element : elements) {
                String link = element.absUrl("href");
                log.info("link: {}", link);

                if (!link.contains("#") && !isFile(link) && link.startsWith(site.getUrl()) &&
                        !processedLinks.contains(link) && element.tagName().equals("a")) {
                    SiteMapRecursiveAction siteMapRecursiveAction = new SiteMapRecursiveAction(site, link,
                            pageRepository, siteRepository, processedLinks, lemmaRepository, indexedRepository);
                    siteMapRecursiveAction.fork();
                    taskList.add(siteMapRecursiveAction);
                }
            }

            for (SiteMapRecursiveAction task : taskList) {
                task.join();
            }

            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

        } catch (InterruptedException | IOException e) {
            site.setLastError(e.getMessage());
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
        }
    }

    public static boolean isFile (String absLink) {
        return absLink.contains(".jpg") || absLink.contains(".png") || absLink.contains(".pdf") ||
                absLink.contains(".jpeg") || absLink.contains(".gif") || absLink.contains(".webp") ||
                absLink.contains(".eps") || absLink.contains(".xlsx") || absLink.contains(".doc") ||
                absLink.contains(".pptx") || absLink.contains(".docx") || absLink.contains("?_ga");
    }
}













