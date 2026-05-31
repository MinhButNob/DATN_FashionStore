package com.example.backend_fashionstore.QuanLySize.service;

import com.example.backend_fashionstore.QuanLySize.dto.SizeRequestDTO;
import com.example.backend_fashionstore.QuanLySize.dto.SizeResponseDTO;

import com.example.backend_fashionstore.QuanLySize.repository.SizeRepository;
import com.example.backend_fashionstore.entity.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SizeServiceImpl implements SizeService {

    private final SizeRepository sizeRepository;

    private SizeResponseDTO convert(Size size){

        return SizeResponseDTO.builder()
                .sizeId(size.getSizeId())
                .sizeCode(size.getSizeCode())
                .sizeName(size.getSizeName())
                .description(size.getDescription())
                .status(size.getStatus())
                .build();
    }

    @Override
    public Page<SizeResponseDTO> getAll(int page, int size) {

        Pageable pageable = PageRequest.of(page,size);

        return sizeRepository.findAll(pageable)
                .map(this::convert);
    }

    @Override
    public SizeResponseDTO getById(Integer id) {

        Size size = sizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

        return convert(size);
    }

    @Override
    public SizeResponseDTO create(SizeRequestDTO request) {

        if(sizeRepository.existsBySizeCode(request.getSizeCode())){
            throw new RuntimeException("Mã size đã tồn tại");
        }

        Size size = Size.builder()
                .sizeCode(request.getSizeCode())
                .sizeName(request.getSizeName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();

        return convert(sizeRepository.save(size));
    }

    @Override
    public SizeResponseDTO update(Integer id, SizeRequestDTO request) {

        Size size = sizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

        size.setSizeCode(request.getSizeCode());
        size.setSizeName(request.getSizeName());
        size.setDescription(request.getDescription());
        size.setStatus(request.getStatus());

        return convert(sizeRepository.save(size));
    }

    @Override
    public void delete(Integer id) {

        Size size = sizeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size không tồn tại"));

        sizeRepository.delete(size);
    }

    @Override
    public Page<SizeResponseDTO> search(String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page,size);

        return sizeRepository
                .findBySizeCodeContainingIgnoreCaseOrSizeNameContainingIgnoreCase(
                        keyword,
                        keyword,
                        pageable
                )
                .map(this::convert);
    }

    @Override
    public long count() {
        return sizeRepository.count();
    }
}