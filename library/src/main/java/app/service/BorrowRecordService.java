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

/**
 * Manages the borrow/return lifecycle of books.
 *
 * Business rules:
 *  - Default loan period: 14 days (DEFAULT_LOAN_DAYS)
 *  - Maximum active borrows per user: 5 (MAX_ACTIVE_BORROWS)
 *  - A user cannot borrow the same book twice simultaneously
 *  - Inventory is decremented on borrow, incremented on return
 *  - All mutating operations are wrapped in @Transactional for atomicity
 */
@Service
public class BorrowRecordService {

    /** How many days a borrowed book stays valid before becoming overdue. */
    private static final int DEFAULT_LOAN_DAYS = 14;

    /** Maximum number of books a single user can have borrowed at once. */
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

    /**
     * Creates a borrow record after validating all business rules.
     * Inventory is atomically decremented — either both the record and the update happen, or neither does.
     *
     * @throws BookNotAvailableException  if no copies are left
     * @throws BorrowLimitException      if the user already has MAX_ACTIVE_BORROWS
     * @throws DuplicateBorrowException  if the user already has this book borrowed
     */
    @Transactional
    public BorrowRecordDto borrowBook(UUID userId, UUID bookId) {
        User user = userService.getUserById(userId);
        Book book = bookService.getBookById(bookId);

        // Rule 1: at least one copy must be available.
        if (book.getCopiesAvailable() <= 0) {
            throw new BookNotAvailableException(
                    "Sorry, no copies of '%s' are currently available.".formatted(book.getTitle()));
        }

        // Rule 2: enforce per-user limit to prevent one user hoarding the catalog.
        long activeCount = borrowRepo.countActiveByUserId(userId);
        if (activeCount >= MAX_ACTIVE_BORROWS) {
            throw new BorrowLimitException(
                    "You've reached the maximum of %d active borrows. Return some books first."
                            .formatted(MAX_ACTIVE_BORROWS));
        }

        // Rule 3: no duplicates — same user can't have the same book twice in their active loans.
        boolean alreadyBorrowed = borrowRepo.findActiveByUserId(userId).stream()
                .anyMatch(b -> b.getBook().getId().equals(bookId));
        if (alreadyBorrowed) {
            throw new DuplicateBorrowException(
                    "You already have an active loan for '%s'.".formatted(book.getTitle()));
        }

        // All checks passed — create the borrow record.
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowDate(now);
        record.setDueDate(now.plusDays(DEFAULT_LOAN_DAYS));
        record.setStatus(BorrowStatus.BORROWED);

        // Decrement inventory atomically (same transaction).
        book.setCopiesAvailable(book.getCopiesAvailable() - 1);
        bookService.saveBook(book);

        return BorrowRecordDto.from(borrowRepo.save(record));
    }

    /**
     * Marks a borrow record as returned.
     * Inventory is incremented back, status set to RETURNED, return date stamped.
     *
     * @throws BorrowNotFoundException    if the record ID doesn't exist
     * @throws AccessDeniedException      if the user tries to return someone else's book
     * @throws IllegalStateException      if the book was already returned
     */
    @Transactional
    public BorrowRecordDto returnBook(UUID userId, UUID recordId) {
        BorrowRecord record = borrowRepo.findById(recordId)
                .orElseThrow(() -> new BorrowNotFoundException("Borrow record not found"));

        // Ownership check: a user can only return books they personally borrowed.
        // Admins bypass this check in the controller layer (not here).
        if (!record.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only return your own borrows");
        }

        // Already returned? Don't allow double-returns (would double-count inventory).
        if (record.getReturnDate() != null) {
            throw new IllegalStateException(
                    "This book was already returned on " + record.getReturnDate());
        }

        // Mark as returned.
        record.setReturnDate(LocalDateTime.now());
        record.setStatus(BorrowStatus.RETURNED);

        // Restore inventory atomically (same transaction).
        Book book = record.getBook();
        book.setCopiesAvailable(book.getCopiesAvailable() + 1);
        bookService.saveBook(book);

        return BorrowRecordDto.from(borrowRepo.save(record));
    }

    /** Returns every borrow record for a user, newest first. */
    public List<BorrowRecordDto> getMyBorrows(UUID userId) {
        return borrowRepo.findByUserIdOrderByBorrowDateDesc(userId).stream()
                .map(BorrowRecordDto::from)
                .collect(Collectors.toList());
    }

    /** Counts how many books the user currently has borrowed (not yet returned). */
    public long getActiveBorrowCount(UUID userId) {
        return borrowRepo.countActiveByUserId(userId);
    }

    /**
     * Counts overdue books for a user.
     * "Overdue" = status is BORROWED AND dueDate is in the past.
     * Computed dynamically — we don't store LATE, it's derived from current time vs dueDate.
     */
    public long getOverdueCount(UUID userId) {
        return borrowRepo.findActiveByUserId(userId).stream()
                .filter(BorrowRecord::isOverdue)
                .count();
    }
}