package searchengine.dto;

import lombok.Data;

@Data
public class IndexedDto {
    private Integer id;
    private float ranking;
    private PageDto pageDto;
    private LemmaDto lemmaDto;
}
