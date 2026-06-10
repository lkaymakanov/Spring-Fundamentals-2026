package app.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fines")
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    private BorrowRecord borrowRecord;
    private Double amount;
    private Boolean paid = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}