package com.example.backend_fashionstore.QuanLySize.service;

import com.example.backend_fashionstore.QuanLySize.dto.SizeRequestDTO;
import com.example.backend_fashionstore.QuanLySize.dto.SizeResponseDTO;
import com.example.backend_fashionstore.QuanLySize.repository.SizeRepository;
import com.example.backend_fashionstore.entity.Size;
import com.example.backend_fashionstore.enums.SizeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SizeServiceImpl implements SizeService {

    private final SizeRepository sizeRepository;

    private SizeResponseDTO convert(Size size) {
        String statusString = null;
        if (size.getStatus() != null) {
            statusString = size.getStatus().name();
        }

        return SizeResponseDTO.builder()
                .sizeId(size.getSizeId())
                .sizeCode(size.getSizeCode())
                .sizeName(size.getSizeName())
                .description(size.getDescription())
                .status(statusString)
                .build();
    }

    @Override
    public Page<SizeResponseDTO> getAll(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && !status.isBlank()) {
            try {
                SizeStatus sizeStatus = SizeStatus.valueOf(status.toUpperCase());
                return sizeRepository.findByStatus(sizeStatus, pageable).map(this::convert);
            } catch (IllegalArgumentException e) {
                // status không hợp lệ -> trả về tất cả
            }
        }
        // Không filter: trả về tất cả (bao gồm DELETED)
        return sizeRepository.findAll(pageable).map(this::convert);
    }

    @Override
    public SizeResponseDTO getById(Integer id) {
        Size size = sizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

        // ✅ Nếu đã xóa thì không cho xem
        if (size.getStatus() == SizeStatus.DELETED) {
            throw new RuntimeException("Size đã bị xóa");
        }

        return convert(size);
    }

    @Override
    public SizeResponseDTO create(SizeRequestDTO request) {
        if (sizeRepository.existsActiveBySizeCode(request.getSizeCode())) {
            throw new RuntimeException("Mã size đã tồn tại");
        }

        // ✅ Chuyển String status sang Enum
        SizeStatus sizeStatus = SizeStatus.ACTIVE;
        if (request.getStatus() != null) {
            try {
                sizeStatus = SizeStatus.valueOf(request.getStatus().toUpperCase());
                // Không cho phép tạo mới với status DELETED
                if (sizeStatus == SizeStatus.DELETED) {
                    sizeStatus = SizeStatus.ACTIVE;
                }
            } catch (IllegalArgumentException e) {
                sizeStatus = SizeStatus.ACTIVE;
            }
        }

        Size size = Size.builder()
                .sizeCode(request.getSizeCode())
                .sizeName(request.getSizeName())
                .description(request.getDescription())
                .status(sizeStatus)
                .build();

        return convert(sizeRepository.save(size));
    }

    @Override
    public SizeResponseDTO update(Integer id, SizeRequestDTO request) {
        Size size = sizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

        // ✅ Không cho sửa size đã xóa
        if (size.getStatus() == SizeStatus.DELETED) {
            throw new RuntimeException("Không thể cập nhật size đã bị xóa");
        }

        if (sizeRepository.existsActiveBySizeCodeAndIdNot(request.getSizeCode(), id)) {
            throw new RuntimeException("Mã size đã tồn tại");
        }

        size.setSizeCode(request.getSizeCode());
        size.setSizeName(request.getSizeName());
        size.setDescription(request.getDescription());

        // ✅ Chuyển String status sang Enum
        if (request.getStatus() != null) {
            try {
                SizeStatus newStatus = SizeStatus.valueOf(request.getStatus().toUpperCase());
                // Không cho phép cập nhật thành DELETED qua update
                if (newStatus != SizeStatus.DELETED) {
                    size.setStatus(newStatus);
                }
            } catch (IllegalArgumentException e) {
                // Giữ nguyên status cũ
            }
        }

        return convert(sizeRepository.save(size));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Size size = sizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

        // ✅ Soft delete - chuyển status thành DELETED
        size.setStatus(SizeStatus.DELETED);
        size.setDeletedAt(LocalDateTime.now());
        sizeRepository.save(size);
    }

    @Override
    public Page<SizeResponseDTO> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // ✅ Chỉ tìm size chưa xóa
        return sizeRepository.searchActive(keyword, pageable).map(this::convert);
    }

    @Override
    public long count() {
        // ✅ Chỉ đếm size chưa xóa
        return sizeRepository.countActive();
    }

    @Override
    @Transactional
    public SizeResponseDTO restore(Integer id) {
        Size size = sizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

        if (size.getStatus() != SizeStatus.DELETED) {
            throw new RuntimeException("Chỉ có thể khôi phục size đã bị xóa");
        }

        size.setStatus(SizeStatus.INACTIVE); // hoặc ACTIVE tùy bạn
        size.setDeletedAt(null);
        Size restoredSize = sizeRepository.save(size);
        return convert(restoredSize);
    }
}