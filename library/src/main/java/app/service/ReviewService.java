package app.service;

import app.model.entity.Review;
import app.repository.ReviewRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    public ReviewService(ReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }
    public Review addReview(Review review) {
        return reviewRepo.save(review);
    }
    public List<Review> getReviewsByBook(UUID bookId) {
        return reviewRepo.findByBookId(bookId);
    }
    public List<Review> getReviewsByUser(UUID userId) {
        return reviewRepo.findByUserId(userId);
    }
    public void deleteReview(UUID id) {
        reviewRepo.deleteById(id);
    }
}