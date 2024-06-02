package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.Indexed;
import searchengine.models.Page;

import java.util.List;

public interface IndexedRepository extends JpaRepository<Indexed, Integer> {
    void deleteByPage(Page page);
    List<Indexed> findByLemmaId(Integer lemmaId);
    List<Indexed> findByLemmaIdAndPageId(Integer lemmaId, Integer pageId);
}
