package app.service;

import app.model.entity.Book;
import app.repository.BookRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepo;

    public BookService(BookRepository bookRepo) {
        this.bookRepo = bookRepo;
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
}