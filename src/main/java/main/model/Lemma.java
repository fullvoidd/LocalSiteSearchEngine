package main.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.List;

/**
 * This class is a DB entity. It contains specific lemma and the frequency of its meeting.
 */
@Entity
@NoArgsConstructor
@Data
@Table(name = "lemma")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private Integer frequency = 0;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(nullable = false, name = "site_id", referencedColumnName = "id")
    private Site site;

    public Lemma(String lemma, Site site, int frequency) {
        this.lemma = lemma;
        this.site = site;
        this.frequency = frequency;
    }
}
