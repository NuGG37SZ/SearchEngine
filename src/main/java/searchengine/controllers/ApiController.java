package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.IndexResponse;
import searchengine.dto.StatisticsResponse;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final SiteService siteService;

    public ApiController(StatisticsService statisticsService, SiteService siteService) {
        this.statisticsService = statisticsService;
        this.siteService = siteService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexResponse> startIndex() {
        return ResponseEntity.ok(siteService.indexSite());
    }

    @GetMapping("/stopIndex")
    public ResponseEntity<IndexResponse> stopIndex() {
        return ResponseEntity.ok(siteService.stopIndexing());
    }

}
