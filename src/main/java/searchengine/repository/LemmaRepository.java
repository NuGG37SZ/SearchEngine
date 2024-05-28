package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.Lemma;
import searchengine.models.Sites;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    void deleteAllBySite(Sites site);
    Integer findLemmaIdByLemma(String lemma);
    Lemma findByLemma(String lemma);
}
