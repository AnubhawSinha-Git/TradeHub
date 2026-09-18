package com.tradehub.review;

import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.order.OrderRepository;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.review.dto.ProductReviewsResponse;
import com.tradehub.review.dto.ReviewRequest;
import com.tradehub.review.dto.ReviewResponse;
import com.tradehub.user.RoleName;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public ProductReviewsResponse getProductReviews(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<Review> reviews =
                reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);

        double averageRating = reviews.isEmpty()
                ? 0.0
                : reviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);

        List<ReviewResponse> content = reviews.stream()
                .map(this::toResponse)
                .toList();

        return new ProductReviewsResponse(
                productId,
                averageRating,
                reviews.size(),
                content);
    }

    @Transactional
    public ProductReviewsResponse addReview(
            String email,
            Long productId,
            ReviewRequest request) {
        User user = findUser(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (orderRepository.existsPurchasedProduct(
                user.getId(), product.getId()) == 0) {
            throw new IllegalArgumentException(
                    "You must purchase the product before reviewing it");
        }

        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw new IllegalArgumentException(
                    "You have already reviewed this product");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(
                request.comment() == null ? null : request.comment().trim());
        reviewRepository.save(review);

        return getProductReviews(productId);
    }

    @Transactional
    public ProductReviewsResponse updateReview(
            String email,
            Long productId,
            ReviewRequest request) {
        User user = findUser(email);

        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Review review = reviewRepository
                .findByProductIdAndUserId(productId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setRating(request.rating());
        review.setComment(
                request.comment() == null ? null : request.comment().trim());

        return getProductReviews(productId);
    }

    @Transactional
    public void deleteReview(String email, Long reviewId) {
        User user = findUser(email);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        boolean isOwner = review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().contains(RoleName.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException(
                    "You cannot delete this review");
        }

        reviewRepository.delete(review);
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ReviewResponse toResponse(Review review) {
        User user = review.getUser();

        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                user.getId(),
                user.getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}