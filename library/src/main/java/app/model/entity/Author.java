package app.model.entity;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(length = 1000)
    private String bio;

    @OneToMany(mappedBy = "author")
    private List<Book> books;
}