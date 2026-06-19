package app.service;

import app.exception.AuthorNotFoundException;
import app.model.dto.AuthorCreateRequest;
import app.model.dto.AuthorEditRequest;
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
                .orElseThrow(() -> new AuthorNotFoundException("Author with id [%s] does not exist.".formatted(id)));
    }

    public Author createAuthor(AuthorCreateRequest request) {
        Author author = new Author();
        author.setName(request.getName());
        author.setBio(request.getBio());
        return authorRepo.save(author);
    }

    public Author updateAuthor(UUID id, AuthorEditRequest request) {
        Author author = getById(id);
        author.setName(request.getName());
        author.setBio(request.getBio());
        return authorRepo.save(author);
    }

    public void deleteAuthor(UUID id) {
        authorRepo.deleteById(id);
    }

    public List<Author> searchByName(String search) {
       return authorRepo.findByNameContainingIgnoreCase(search);
    }
}