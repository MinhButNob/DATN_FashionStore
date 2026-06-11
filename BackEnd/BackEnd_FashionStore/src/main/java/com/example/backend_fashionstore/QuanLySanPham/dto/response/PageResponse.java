package com.example.backend_fashionstore.QuanLySanPham.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page, size, totalPages;
    private long totalElements;

    public static <T> PageResponse<T> from(Page<T> p) {
        return PageResponse.<T>builder()
                .content(p.getContent())
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .build();
    }
}