package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.Indexed;
import searchengine.models.Page;

import java.util.Set;

public interface IndexedRepository extends JpaRepository<Indexed, Integer> {
    void deleteByPage(Page page);
    Set<Page> findPageByLemmaId(Integer lemma);
    float findRankingByLemmaIdAndPage(Integer lemma, Page page);
}
