package app.service;

import app.model.entity.Author;
import app.repository.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthorService {

    private final AuthorRepository authorRepo;

    public AuthorService(AuthorRepository authorRepo) {
        this.authorRepo = authorRepo;
    }

    public List<Author> getAllAuthors() {
        return authorRepo.findAll();
    }

    public Author getById(UUID id) {
        return authorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found"));
    }
}