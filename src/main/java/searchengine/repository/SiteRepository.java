package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.models.Sites;

@Repository
public interface SiteRepository extends JpaRepository<Sites, Long> {
    Sites findByUrl(String url);
    String findNameByUrl(String url);
}
