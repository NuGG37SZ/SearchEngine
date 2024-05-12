
package searchengine.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import searchengine.enums.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "site")
public class Sites {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
