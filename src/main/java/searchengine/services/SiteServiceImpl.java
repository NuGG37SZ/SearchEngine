package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.IndexResponse;
import org.springframework.stereotype.Service;
import searchengine.enums.Status;
import searchengine.models.Sites;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;


@Slf4j
@Service
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private IndexResponse response = new IndexResponse();
    private ForkJoinPool forkJoinPool = new ForkJoinPool();

    public SiteServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, SitesList sitesList) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
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
            ConcurrentSkipListSet<String> processedLinks = new ConcurrentSkipListSet<>();
            SiteMapRecursiveAction task =
                    new SiteMapRecursiveAction(sites, sites.getUrl(), pageRepository, siteRepository, processedLinks);
            forkJoinPool.execute(task);
            try {
                task.get();
                sites.setStatus(Status.INDEXED);
                sites.setStatusTime(LocalDateTime.now());
                siteRepository.save(sites);
            } catch (Exception e) {
                sites.setStatus(Status.FAILED);
                sites.setLastError(e.getMessage());
                siteRepository.save(sites);
                response.setResult(false);
                response.setError(e.getMessage());
            }

        }
        response.setError("");
        response.setResult(true);
        return response;
    }

    @Override
    public IndexResponse stopIndexing() {
        return null;
    }

}

