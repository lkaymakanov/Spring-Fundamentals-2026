package app.service;

import app.exception.AuthorNotFoundException;
import app.model.dto.AuthorCreateRequest;
import app.model.dto.AuthorEditRequest;
import app.model.entity.Author;
import app.repository.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Manages authors in the library catalog.
 * Author records are referenced by Book entities (one author can have many books).
 */
@Service
public class AuthorService {

    private final AuthorRepository authorRepo;

    public AuthorService(AuthorRepository authorRepo) {
        this.authorRepo = authorRepo;
    }

    /** Returns every author in the catalog (alphabetical sort depends on repository). */
    public List<Author> getAllAuthors() {
        return authorRepo.findAll();
    }

    /**
     * Looks up an author by ID.
     * @throws AuthorNotFoundException if no author matches the given ID
     */
    public Author getById(UUID id) {
        return authorRepo.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException("Author with id [%s] does not exist.".formatted(id)));
    }

    /**
     * Creates a new author from the registration form data.
     * Returns the persisted entity (with its generated ID).
     */
    public Author createAuthor(AuthorCreateRequest request) {
        Author author = new Author();
        author.setName(request.getName());
        author.setBio(request.getBio());
        return authorRepo.save(author);
    }

    /**
     * Updates an existing author's name and biography.
     * @throws AuthorNotFoundException if no author matches the given ID
     */
    public Author updateAuthor(UUID id, AuthorEditRequest request) {
        Author author = getById(id);
        author.setName(request.getName());
        author.setBio(request.getBio());
        return authorRepo.save(author);
    }

    /**
     * Deletes an author by ID.
     * Note: callers (controllers) should verify no books reference this author
     * before calling, otherwise books will end up with a null author pointer.
     */
    public void deleteAuthor(UUID id) {
        authorRepo.deleteById(id);
    }

    /** Case-insensitive search by name. Returns an empty list if no matches. */
    public List<Author> searchByName(String search) {
        return authorRepo.findByNameContainingIgnoreCase(search);
    }
}