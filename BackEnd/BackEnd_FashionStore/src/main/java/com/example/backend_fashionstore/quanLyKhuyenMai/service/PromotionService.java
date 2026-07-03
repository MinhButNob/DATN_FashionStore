package com.example.backend_fashionstore.quanLyKhuyenMai.service;

import com.example.backend_fashionstore.entity.Promotion;
import com.example.backend_fashionstore.quanLyKhuyenMai.dto.*;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    // Lấy danh sách (chỉ hiển thị chưa xóa)
    @Transactional
    public Page<PromotionDTO> getAllPromotions(Pageable pageable) {
        autoDeactivateExpiredPromotions();
        return promotionRepository.findAllActive(pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public Page<PromotionDTO> searchPromotions(String keyword, Pageable pageable) {
        autoDeactivateExpiredPromotions();
        return promotionRepository.searchByPromotionName(keyword, pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public Page<PromotionDTO> filterPromotionsByStatus(String status, Pageable pageable) {
        autoDeactivateExpiredPromotions();
        return promotionRepository.findByPromotionStatus(status, pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public Page<PromotionDTO> filterAndSearchPromotions(String status, String keyword, Pageable pageable) {
        autoDeactivateExpiredPromotions();
        if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty()) {
            return promotionRepository.searchByPromotionStatusAndName(status, keyword, pageable)
                    .map(this::convertToDTO);
        } else if (keyword != null && !keyword.isEmpty()) {
            return searchPromotions(keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            return filterPromotionsByStatus(status, pageable);
        } else {
            return getAllPromotions(pageable);
        }
    }

    // Lấy danh sách đã xóa (soft delete)
    @Transactional
    public Page<PromotionDTO> getDeletedPromotions(Pageable pageable) {
        autoDeactivateExpiredPromotions();
        return promotionRepository.findDeletedPromotions(pageable)
                .map(this::convertToDTO);
    }

    public PromotionDTO getPromotionById(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));
        return convertToDTO(promotion);
    }

    @Transactional
    public PromotionDTO createPromotion(PromotionCreateDTO dto) {
        if (promotionRepository.existsByPromotionName(dto.getPromotionName())) {
            throw new RuntimeException("Tên khuyến mãi đã tồn tại: " + dto.getPromotionName());
        }

        if (dto.getPromotionEndDate().isBefore(dto.getPromotionStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Promotion promotion = Promotion.builder()
                .promotionName(dto.getPromotionName())
                .promotionDescription(dto.getPromotionDescription())
                .promotionStartDate(dto.getPromotionStartDate())
                .promotionEndDate(dto.getPromotionEndDate())
                .promotionDiscountPercent(dto.getPromotionDiscountPercent() != null ?
                        dto.getPromotionDiscountPercent() : BigDecimal.ZERO)
                .promotionStatus("ACTIVE")
                .build();
        promotion = promotionRepository.save(promotion);
        return convertToDTO(promotion);
    }

    @Transactional
    public PromotionDTO updatePromotion(Integer id, PromotionUpdateDTO dto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        if (promotion.getPromotionDeletedAt() != null) {
            throw new RuntimeException("Khuyến mãi đã bị xóa, không thể cập nhật");
        }

        if (promotionRepository.existsByPromotionNameAndIdNot(dto.getPromotionName(), id)) {
            throw new RuntimeException("Tên khuyến mãi đã tồn tại: " + dto.getPromotionName());
        }

        if (dto.getPromotionEndDate().isBefore(dto.getPromotionStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        promotion.setPromotionName(dto.getPromotionName());
        promotion.setPromotionDescription(dto.getPromotionDescription());
        promotion.setPromotionStartDate(dto.getPromotionStartDate());
        promotion.setPromotionEndDate(dto.getPromotionEndDate());
        promotion.setPromotionDiscountPercent(dto.getPromotionDiscountPercent() != null ?
                dto.getPromotionDiscountPercent() : BigDecimal.ZERO);
        promotion.setPromotionUpdatedAt(LocalDateTime.now());
        promotion = promotionRepository.save(promotion);
        return convertToDTO(promotion);
    }

    // Xóa mềm (ẩn)
    @Transactional
    public void softDeletePromotion(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        if (promotion.getPromotionDeletedAt() != null) {
            throw new RuntimeException("Khuyến mãi đã bị xóa trước đó");
        }

        promotion.setPromotionDeletedAt(LocalDateTime.now());
        promotion.setPromotionUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    // Khôi phục
    @Transactional
    public void restorePromotion(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        if (promotion.getPromotionDeletedAt() == null) {
            throw new RuntimeException("Khuyến mãi chưa bị xóa, không cần khôi phục");
        }

        promotion.setPromotionDeletedAt(null);
        promotion.setPromotionUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    @Transactional
    public void togglePromotionStatus(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        if (promotion.getPromotionDeletedAt() != null) {
            throw new RuntimeException("Khuyến mãi đã bị xóa, không thể thay đổi trạng thái");
        }

        if ("ACTIVE".equals(promotion.getPromotionStatus())) {
            promotion.setPromotionStatus("INACTIVE");
        } else {
            if (promotion.getPromotionEndDate().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Khuyến mãi đã hết hạn, không thể kích hoạt. Vui lòng cập nhật lại ngày kết thúc trước.");
            }
            promotion.setPromotionStatus("ACTIVE");
        }
        promotion.setPromotionUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    @Transactional
    public PromotionStatisticsDTO getStatistics() {
        autoDeactivateExpiredPromotions();
        long total = promotionRepository.countAllActive();
        long active = promotionRepository.countByStatusActive();
        long inactive = promotionRepository.countByStatusInactive();

        return new PromotionStatisticsDTO(total, active, inactive);
    }

    private PromotionDTO convertToDTO(Promotion p) {
        PromotionDTO dto = new PromotionDTO();
        dto.setPromotionId(p.getPromotionId());
        dto.setPromotionName(p.getPromotionName());
        dto.setPromotionDescription(p.getPromotionDescription());
        dto.setPromotionStartDate(p.getPromotionStartDate());
        dto.setPromotionEndDate(p.getPromotionEndDate());
        dto.setPromotionDiscountPercent(p.getPromotionDiscountPercent());
        dto.setPromotionStatus(p.getPromotionStatus());
        dto.setPromotionCreatedAt(p.getPromotionCreatedAt());
        dto.setPromotionUpdatedAt(p.getPromotionUpdatedAt());
        dto.setPromotionDeletedAt(p.getPromotionDeletedAt());
        return dto;
    }

    @Transactional
    public void autoDeactivateExpiredPromotions() {
        promotionRepository.deactivateExpiredPromotions(LocalDateTime.now());
    }
}