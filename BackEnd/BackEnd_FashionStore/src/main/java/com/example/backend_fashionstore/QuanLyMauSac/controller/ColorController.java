package com.example.backend_fashionstore.QuanLyMauSac.controller;

import com.example.backend_fashionstore.QuanLyMauSac.dto.request.ColorRequestDTO;
import com.example.backend_fashionstore.QuanLyMauSac.dto.response.ColorResponseDTO;
import com.example.backend_fashionstore.QuanLyMauSac.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colors")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public ResponseEntity<Page<ColorResponseDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(colorService.getAll(page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ColorResponseDTO>> getAllActive() {
        return ResponseEntity.ok(colorService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColorResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(colorService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ColorResponseDTO> create(@RequestBody ColorRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(colorService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColorResponseDTO> update(
            @PathVariable Integer id,
            @RequestBody ColorRequestDTO request
    ) {
        return ResponseEntity.ok(colorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        colorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Integer id) {
        colorService.restore(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ColorResponseDTO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(colorService.search(keyword, page, size));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        return ResponseEntity.ok(colorService.count());
    }
}