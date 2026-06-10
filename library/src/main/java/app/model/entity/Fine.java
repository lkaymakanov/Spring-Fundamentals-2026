package app.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fines")
@Data
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    private BorrowRecord borrowRecord;
    private BigDecimal amount;
    private Boolean paid = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}