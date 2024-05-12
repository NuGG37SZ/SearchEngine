
package searchengine.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "page")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Sites site;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Lob
    @Column(nullable = false)
    private String content;

}
