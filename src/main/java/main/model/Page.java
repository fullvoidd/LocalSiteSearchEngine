package main.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.List;

/**
 * This class is a DB entity. It contains path to the page from the main page of the site,
 * and also status code and content.
 */
@Entity
@NoArgsConstructor
@Data
@Table(name = "page", indexes = @Index(columnList = "path"))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "mediumtext")
    private String content;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(nullable = false, name = "site_id")
    private Site site;

    public Page(String content, int code, String path, Site site) {
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }

    public Page(int id, String content, int code, String path, Site site) {
        this.id = id;
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }
}
