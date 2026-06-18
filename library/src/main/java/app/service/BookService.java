package app.service;

import app.model.dto.BookCreateRequest;
import app.model.dto.BookEditRequest;
import app.model.entity.Author;
import app.model.entity.Book;
import app.repository.BookRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookService {



    private final BookRepository bookRepo;
    private final AuthorService authorService;

    public BookService(BookRepository bookRepo, AuthorService authorService) {
        this.bookRepo = bookRepo;
        this.authorService = authorService;
    }

    public Book getBookById(UUID id) {
        return bookRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));
    }



    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }
    public Book saveBook(Book book) {
        return bookRepo.save(book);
    }
    public void deleteBook(UUID id) {
        bookRepo.deleteById(id);
    }
    public List<Book> searchByTitle(String title) {
        return bookRepo.findByTitleContainingIgnoreCase(title);
    }

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

    public Book updateBook(UUID id, BookEditRequest request) {
        Book book = getBookById(id);
        Author author = authorService.getById(request.getAuthorId());

        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setPublicationYear(request.getPublicationYear());
        book.setCopiesAvailable(request.getCopiesAvailable());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setAuthor(author);

        return bookRepo.save(book);
    }}