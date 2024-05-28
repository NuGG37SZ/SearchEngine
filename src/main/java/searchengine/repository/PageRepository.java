package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.models.Page;
import searchengine.models.Sites;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findBySiteAndPath(Sites site, String path);
    Optional<Page> findByPath(String path);
    String findByContent(Page page);
}
