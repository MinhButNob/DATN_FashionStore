package com.example.backend_fashionstore.QuanLySize.controller;

import com.example.backend_fashionstore.QuanLySize.dto.SizeRequestDTO;
import com.example.backend_fashionstore.QuanLySize.dto.SizeResponseDTO;
import com.example.backend_fashionstore.QuanLySize.service.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sizes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SizeController {

    private final SizeService sizeService;

    @GetMapping
    public Page<SizeResponseDTO> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return sizeService.getAll(page,size);
    }

    @GetMapping("/{id}")
    public SizeResponseDTO getById(@PathVariable Integer id){
        return sizeService.getById(id);
    }

    @PostMapping
    public SizeResponseDTO create(
            @RequestBody SizeRequestDTO request
    ){
        return sizeService.create(request);
    }

    @PutMapping("/{id}")
    public SizeResponseDTO update(
            @PathVariable Integer id,
            @RequestBody SizeRequestDTO request
    ){
        return sizeService.update(id,request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Integer id
    ){
        sizeService.delete(id);
    }

    @GetMapping("/search")
    public Page<SizeResponseDTO> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return sizeService.search(keyword,page,size);
    }

    @GetMapping("/count")
    public long count(){
        return sizeService.count();
    }
}