package searchengine.dto;

import lombok.Data;

@Data
public class PageDto {
    private Integer id;
    private Integer siteId;
    private String path;
    private Integer code;
    private String content;

    public PageDto(Integer siteId, String path, Integer code, String content) {
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public PageDto() {}
}
