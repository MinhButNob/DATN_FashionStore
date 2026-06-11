package com.example.backend_fashionstore.QuanLyMauSac.service;

import com.example.backend_fashionstore.QuanLyMauSac.dto.request.ColorRequestDTO;
import com.example.backend_fashionstore.QuanLyMauSac.dto.response.ColorResponseDTO;
import com.example.backend_fashionstore.QuanLyMauSac.repository.ColorRepository;
import com.example.backend_fashionstore.entity.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColorServiceImpl implements ColorService {

    private final ColorRepository colorRepository;

    private ColorResponseDTO convertToResponse(Color color) {
        return ColorResponseDTO.builder()
                .colorId(color.getColorId())
                .colorCode(color.getColorCode())
                .colorName(color.getColorName())
                .hexCode(color.getHexCode())
                .status(color.getStatus())
                .build();
    }

    @Override
    public Page<ColorResponseDTO> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("colorId").ascending());
        return colorRepository.findAllActive(pageable).map(this::convertToResponse);
    }

    @Override
    public List<ColorResponseDTO> getAllActive() {
        return colorRepository.findActiveColors()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ColorResponseDTO getById(Integer id) {
        Color color = colorRepository.findByColorIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu với ID: " + id));
        return convertToResponse(color);
    }

    @Override
    @Transactional
    public ColorResponseDTO create(ColorRequestDTO request) {
        if (colorRepository.existsByColorCodeAndDeletedAtIsNull(request.getColorCode())) {
            throw new RuntimeException("Mã màu '" + request.getColorCode() + "' đã tồn tại");
        }

        String status = request.getStatus() != null ? request.getStatus() : "ACTIVE";
        if (!status.matches("ACTIVE|INACTIVE")) {
            status = "ACTIVE";
        }

        Color color = Color.builder()
                .colorCode(request.getColorCode().toUpperCase())
                .colorName(request.getColorName())
                .hexCode(request.getHexCode())
                .status(status)
                .build();

        Color saved = colorRepository.save(color);
        log.info("Tạo màu thành công với ID: {}", saved.getColorId());

        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public ColorResponseDTO update(Integer id, ColorRequestDTO request) {
        // ✅ Chỉ cho phép update khi chưa bị xóa
        Color color = colorRepository.findByColorIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu với ID: " + id + " hoặc đã bị xóa. Vui lòng khôi phục trước khi sửa."));

        if (!color.getColorCode().equals(request.getColorCode())
                && colorRepository.existsByColorCodeAndDeletedAtIsNull(request.getColorCode())) {
            throw new RuntimeException("Mã màu '" + request.getColorCode() + "' đã tồn tại");
        }

        color.setColorCode(request.getColorCode().toUpperCase());
        color.setColorName(request.getColorName());
        color.setHexCode(request.getHexCode());

        if (request.getStatus() != null) {
            color.setStatus(request.getStatus());
        }

        Color updated = colorRepository.save(color);
        log.info("Cập nhật màu thành công với ID: {}", id);

        return convertToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        int deletedCount = colorRepository.softDelete(id, LocalDateTime.now());
        if (deletedCount == 0) {
            throw new RuntimeException("Không thể xóa màu với ID: " + id);
        }
        log.info("Đã xóa mềm màu với ID: {}", id);
    }

    @Override
    @Transactional
    public void restore(Integer id) {
        int restoredCount = colorRepository.restore(id);
        if (restoredCount == 0) {
            throw new RuntimeException("Không thể khôi phục màu với ID: " + id);
        }
        log.info("Đã khôi phục màu với ID: {}", id);
    }

    @Override
    public Page<ColorResponseDTO> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("colorName").ascending());
        return colorRepository.searchByKeyword(keyword, pageable)
                .map(this::convertToResponse);
    }

    @Override
    public long count() {
        return colorRepository.countByDeletedAtIsNull();
    }
}