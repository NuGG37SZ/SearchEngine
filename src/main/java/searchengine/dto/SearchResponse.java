package searchengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private String error;
    private Integer count;
    private List<Datas> data;

    public SearchResponse(boolean result, String error, Integer count, List<Datas> data) {
        this.result = result;
        this.error = error;
        this.count = count;
        this.data = data;
    }
}
