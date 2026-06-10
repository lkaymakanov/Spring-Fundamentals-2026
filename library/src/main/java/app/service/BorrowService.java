package app.service;

import app.model.entity.*;
import app.repository.BookRepository;
import app.repository.BorrowRecordRepository;
import app.repository.FineRepository;
import app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static app.model.entity.BorrowStatus.*;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRepo;
    private final BookRepository bookRepo;
    private final UserRepository userRepo;
    private final FineRepository fineRepo;

    @Autowired
    public BorrowService(BorrowRecordRepository borrowRepo,
                         BookRepository bookRepo,
                         UserRepository userRepo,
                         FineRepository fineRepo) {
        this.borrowRepo = borrowRepo;
        this.bookRepo = bookRepo;
        this.userRepo = userRepo;
        this.fineRepo = fineRepo;
    }

    @Transactional
    public String borrowBook(UUID userId, UUID bookId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if (book.getCopiesAvailable() <= 0) {
            return "No copies available";
        }

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(14));
        record.setStatus(BorrowStatus.BORROWED);


        borrowRepo.save(record);

        book.setCopiesAvailable(book.getCopiesAvailable() - 1);
        bookRepo.save(book);

        return "Book borrowed successfully";
    }

    @Transactional
    public String returnBook(UUID borrowId) {

        BorrowRecord record = borrowRepo.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("Borrow record not found"));

        if (!(record.getStatus() == BORROWED)) {
            return "Book already returned";
        }

        record.setReturnDate(LocalDateTime.now());
        record.setStatus(RETURNED);

        Book book = record.getBook();
        book.setCopiesAvailable(book.getCopiesAvailable() + 1);
        bookRepo.save(book);

        // Check for fine
        if (record.getReturnDate().isAfter(record.getDueDate())) {
            long daysLate = java.time.temporal.ChronoUnit.DAYS.between(
                    record.getDueDate(),
                    record.getReturnDate()
            );

            double fineAmount = daysLate * 1.0;

            Fine fine = new Fine();
            fine.setBorrowRecord(record);
            fine.setAmount(fineAmount);
            fine.setPaid(false);

            fineRepo.save(fine);

            record.setStatus(LATE);
        }

        borrowRepo.save(record);

        return "Book returned successfully";
    }
}
