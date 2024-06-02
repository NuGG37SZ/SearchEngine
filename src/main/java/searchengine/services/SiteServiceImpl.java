package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.IndexResponse;
import org.springframework.stereotype.Service;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;

import static java.lang.Thread.sleep;

@Slf4j
@Service
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final LemmaRepository lemmaRepository;
    private final IndexedRepository indexedRepository;
    private final IndexResponse response = new IndexResponse();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public SiteServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                           SitesList sitesList, LemmaRepository lemmaRepository, IndexedRepository indexedRepository)
    {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexedRepository = indexedRepository;
        this.sitesList = sitesList;
    }

    @Override
    public IndexResponse indexSite() {
        List<Sites> siteList = new ArrayList<>();

        for (Site site : sitesList.getSites()) {
            Sites sites = siteRepository.findByUrl(site.getUrl());

            if (sites != null) {
                siteRepository.delete(sites);
            }

            Sites newSites = new Sites();
            newSites.setUrl(site.getUrl());
            newSites.setName(site.getName());
            newSites.setLastError(null);
            newSites.setStatus(Status.INDEXED);
            newSites.setStatusTime(LocalDateTime.now());
            siteList.add(newSites);
        }
        siteRepository.saveAll(siteList);

        for (Sites sites : siteList) {
            if (!forkJoinPool.isShutdown() && !forkJoinPool.isTerminating()) {
                ConcurrentSkipListSet<String> processedLinks = new ConcurrentSkipListSet<>();
                SiteMapRecursiveAction task = new SiteMapRecursiveAction(sites, sites.getUrl(), pageRepository,
                        siteRepository, processedLinks, lemmaRepository, indexedRepository);
                forkJoinPool.execute(task);
            }
        }
        response.setError("");
        response.setResult(true);
        return response;
    }

    @Override
    public IndexResponse stopIndexing() {
        try {
            forkJoinPool.shutdownNow();
            response.setError("");
            response.setResult(true);
        } catch (Exception e) {
            response.setError("Ошибка при остановке индексации: " + e.getMessage());
            response.setResult(false);
        }
        return response;
    }

    @Override
    @Transactional
    public IndexResponse indexPage(String url) {
        List<Sites> allSites = siteRepository.findAll();

        response.setError("");
        response.setResult(true);

        try {
            boolean isPageFound = false;

            for (Sites site : allSites) {
                if (url.startsWith(site.getUrl())) {
                    isPageFound = true;

                    Optional<Page> pageOptional = pageRepository.findBySiteAndPath(site, url);
                    Page page;

                    Connection connection = Jsoup.connect(url)
                            .ignoreHttpErrors(true)
                            .timeout(5000)
                            .followRedirects(false);
                    Document document = connection.get();

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
                    break;
                }
            }
            if (!isPageFound) {
                response.setError("Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");
                response.setResult(false);
            }
        } catch (IOException ex) {
            response.setError(ex.getMessage());
            response.setResult(false);
        }
        return response;
    }


}

