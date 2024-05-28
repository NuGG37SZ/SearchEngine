package searchengine.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class LemmaDto {
    private Integer id;
    private String lemma;
    private Integer frequency;
    private SitesDto sitesDto;
    private Set<IndexedDto> indexes = new HashSet<>();
}
