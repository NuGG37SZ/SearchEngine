package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.IndexResponse;
import searchengine.dto.SearchResponse;
import searchengine.dto.StatisticsResponse;
import searchengine.services.SearchService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final SiteService siteService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, SiteService siteService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.siteService = siteService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexResponse> startIndex() {
        return ResponseEntity.ok(siteService.indexSite());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexResponse> stopIndex() {
        return ResponseEntity.ok(siteService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(siteService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search (
            @RequestParam(name = "query") String query,
            @RequestParam(name = "site", required = false) String site,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    )
    {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }

}
