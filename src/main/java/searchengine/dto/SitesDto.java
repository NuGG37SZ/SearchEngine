package searchengine.dto;

import lombok.Data;
import searchengine.enums.Status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SitesDto {
    private Integer id;
    private Status status;
    private LocalDateTime statusTime;
    private String lastError;
    private String url;
    private String name;
    private List<PageDto> pages = new ArrayList<>();

    public SitesDto(Status status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public SitesDto() {}
}
