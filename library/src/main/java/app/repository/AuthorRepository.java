package app.repository;

import app.model.entity.Author;
import app.model.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<Author, UUID> {

    List<Author> findByNameContainingIgnoreCase(String title);
}