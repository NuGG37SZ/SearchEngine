package searchengine.services;

import searchengine.dto.IndexResponse;

public interface SiteService {
    IndexResponse indexSite();
    IndexResponse stopIndexing();
    IndexResponse indexPage();
}
