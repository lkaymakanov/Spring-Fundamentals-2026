package app.repository;

import app.model.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, UUID> {

    List<BorrowRecord> findByUserId(UUID userId);
    List<BorrowRecord> findByBookId(UUID bookId);




    List<BorrowRecord> findByUserIdOrderByBorrowDateDesc(UUID userId);

    @Query("SELECT b FROM BorrowRecord b WHERE b.user.id = :userId AND b.returnDate IS NULL")
    List<BorrowRecord> findActiveByUserId(UUID userId);

    @Query("SELECT COUNT(b) FROM BorrowRecord b WHERE b.user.id = :userId AND b.returnDate IS NULL")
    long countActiveByUserId(UUID userId);

    @Query("SELECT COUNT(b) FROM BorrowRecord b WHERE b.status = 'LATE'")
    long countOverdue();

}