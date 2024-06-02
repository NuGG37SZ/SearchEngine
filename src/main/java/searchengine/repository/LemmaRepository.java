package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.Lemma;
import searchengine.models.Sites;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    void deleteAllBySite(Sites site);
    Lemma findByLemma(String lemma);
}
