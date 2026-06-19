package app.service;

import app.exception.*;
import app.model.dto.BorrowRecordDto;
import app.model.entity.Book;
import app.model.entity.BorrowRecord;
import app.model.entity.BorrowStatus;
import app.model.entity.User;
import app.repository.BorrowRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BorrowRecordService {

    private static final int DEFAULT_LOAN_DAYS = 14;
    private static final int MAX_ACTIVE_BORROWS = 5;

    private final BorrowRecordRepository borrowRepo;
    private final BookService bookService;
    private final UserService userService;

    public BorrowRecordService(BorrowRecordRepository borrowRepo,
                               BookService bookService,
                               UserService userService) {
        this.borrowRepo = borrowRepo;
        this.bookService = bookService;
        this.userService = userService;
    }

    @Transactional
    public BorrowRecordDto borrowBook(UUID userId, UUID bookId) {
        User user = userService.getUserById(userId);
        Book book = bookService.getBookById(bookId);

        // 1. Check available copies
        if (book.getCopiesAvailable() <= 0) {
            throw new BookNotAvailableException(
                    "Sorry, no copies of '%s' are currently available.".formatted(book.getTitle()));
        }

        // 2. Check user's active borrow limit
        long activeCount = borrowRepo.countActiveByUserId(userId);
        if (activeCount >= MAX_ACTIVE_BORROWS) {
            throw new BorrowLimitException(
                    "You've reached the maximum of %d active borrows. Return some books first."
                            .formatted(MAX_ACTIVE_BORROWS));
        }

        // 3. Check duplicates
        boolean alreadyBorrowed = borrowRepo.findActiveByUserId(userId).stream()
                .anyMatch(b -> b.getBook().getId().equals(bookId));
        if (alreadyBorrowed) {
            throw new DuplicateBorrowException(
                    "You already have an active loan for '%s'.".formatted(book.getTitle()));
        }

        // 4. Create the borrow record
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowDate(now);
        record.setDueDate(now.plusDays(DEFAULT_LOAN_DAYS));
        record.setStatus(BorrowStatus.BORROWED);

        // 5. Decrement available copies
        book.setCopiesAvailable(book.getCopiesAvailable() - 1);
        bookService.saveBook(book);

        return BorrowRecordDto.from(borrowRepo.save(record));
    }

    @Transactional
    public BorrowRecordDto returnBook(UUID userId, UUID recordId) {
        BorrowRecord record = borrowRepo.findById(recordId)
                .orElseThrow(() -> new BorrowNotFoundException("Borrow record not found"));

        if (!record.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only return your own borrows");
        }

        if (record.getReturnDate() != null) {
            throw new IllegalStateException(
                    "This book was already returned on " + record.getReturnDate());
        }

        // Mark returned
        record.setReturnDate(LocalDateTime.now());
        record.setStatus(BorrowStatus.RETURNED);

        // Increment copies
        Book book = record.getBook();
        book.setCopiesAvailable(book.getCopiesAvailable() + 1);
        bookService.saveBook(book);

        return BorrowRecordDto.from(borrowRepo.save(record));
    }

    public List<BorrowRecordDto> getMyBorrows(UUID userId) {
        return borrowRepo.findByUserIdOrderByBorrowDateDesc(userId).stream()
                .map(BorrowRecordDto::from)
                .collect(Collectors.toList());
    }

    public long getActiveBorrowCount(UUID userId) {
        return borrowRepo.countActiveByUserId(userId);
    }

    public long getOverdueCount(UUID userId) {
        return 0; /* borrowRepo.findActiveByUserId(userId).stream()
                .filter(BorrowRecord::isOverdue)
                .count();*/
    }
}