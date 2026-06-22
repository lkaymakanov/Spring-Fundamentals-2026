package app.service;


import app.model.dto.BookCreateRequest;
import app.model.dto.BookEditRequest;
import app.model.entity.Author;
import app.model.entity.Book;
import app.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Manages books in the library catalog.
 * Books reference an Author (one author → many books).
 * Inventory is tracked via copiesAvailable (decremented on borrow, incremented on return).
 */
@Service
public class BookService {

    private final BookRepository bookRepo;
    private final AuthorService authorService;

    public BookService(BookRepository bookRepo, AuthorService authorService) {
        this.bookRepo = bookRepo;
        this.authorService = authorService;
    }

    /** Returns every book in the catalog, ordered by repository defaults. */
    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }

    /**
     * Looks up a book by ID.
     * @throws RuntimeException if no book matches the given ID
     */
    public Book getBookById(UUID id) {
        return bookRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Book with id [%s] does not exist.".formatted(id)));
    }

    /**
     * Creates a new book entry. Resolves the author from the given authorId
     * — if the author doesn't exist, the authorService throws and the book is not created.
     */
    public Book createBook(BookCreateRequest request) {
        Author author = authorService.getById(request.getAuthorId());

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setPublicationYear(request.getPublicationYear());
        book.setCopiesAvailable(request.getCopiesAvailable());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setAuthor(author);
        return bookRepo.save(book);
    }

    /**
     * Updates an existing book's metadata.
     * Author relationship is updated if a new authorId is provided.
     * Inventory (copiesAvailable) is preserved unless explicitly changed.
     */
    public Book updateBook(UUID id, BookEditRequest request) {
        Book book = getBookById(id);
        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setPublicationYear(request.getPublicationYear());
        book.setCopiesAvailable(request.getCopiesAvailable());
        book.setCoverImageUrl(request.getCoverImageUrl());

        // Allow changing the author by passing a different authorId in the edit form.
        if (request.getAuthorId() != null) {
            Author author = authorService.getById(request.getAuthorId());
            book.setAuthor(author);
        }
        return bookRepo.save(book);
    }

    /**
     * Deletes a book by ID.
     * Caller must ensure no active BorrowRecords reference this book first,
     * otherwise foreign key constraints will fail (or orphans will remain).
     */
    public void deleteBook(UUID id) {
        bookRepo.deleteById(id);
    }

    /** Case-insensitive title search. Returns an empty list if no matches. */
    public List<Book> searchByTitle(String search) {
        return bookRepo.findByTitleContainingIgnoreCase(search);
    }

    /**
     * Persists a Book entity. Used by other services (e.g. BorrowService)
     * that need to update inventory counts after a borrow/return operation.
     */
    public Book saveBook(Book book) {
        return bookRepo.save(book);
    }
}