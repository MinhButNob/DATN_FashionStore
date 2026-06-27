package com.example.backend_fashionstore.review.controller;

import com.example.backend_fashionstore.review.dto.response.AdminReviewResponseDTO;
import com.example.backend_fashionstore.review.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    public ResponseEntity<Page<AdminReviewResponseDTO>> getAllReviews(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminReviewResponseDTO> reviews = adminReviewService.getAllReviews(status, keyword, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<AdminReviewResponseDTO> getReviewDetail(@PathVariable Integer reviewId) {
        return ResponseEntity.ok(adminReviewService.getReviewDetail(reviewId));
    }

    @PutMapping("/{reviewId}/approve")
    public ResponseEntity<AdminReviewResponseDTO> approveReview(@PathVariable Integer reviewId) {
        return ResponseEntity.ok(adminReviewService.approveReview(reviewId));
    }

    @PutMapping("/{reviewId}/reject")
    public ResponseEntity<AdminReviewResponseDTO> rejectReview(
            @PathVariable Integer reviewId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(adminReviewService.rejectReview(reviewId, reason));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer reviewId) {
        adminReviewService.deleteReview(reviewId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getReviewStatistics() {
        return ResponseEntity.ok(adminReviewService.getReviewStatistics());
    }
}