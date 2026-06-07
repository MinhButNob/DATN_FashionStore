package com.example.backend_fashionstore.quanlyNhanVien.controller;

import com.example.backend_fashionstore.QuanLyRole.dto.RoleResponseDTO;
import com.example.backend_fashionstore.QuanLyRole.service.RoleService;
import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.quanlyNhanVien.dto.EmployeeCreateRequest;
import com.example.backend_fashionstore.quanlyNhanVien.dto.EmployeeMapper;
import com.example.backend_fashionstore.quanlyNhanVien.dto.EmployeeResponse;
import com.example.backend_fashionstore.quanlyNhanVien.dto.EmployeeUpdateRequest;
import com.example.backend_fashionstore.quanlyNhanVien.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final SaveServiceEmployee saveService;
    private final UpdateServiceEmployee updateService;
    private final RemoveServiceEmployee removeService;
    private final PageServiceEmployee pageService;
    private final TimKiemServiceEmployee timKiemService;
    private final EmployeeMapper employeeMapper;
    private final RoleService roleService;

    private Map<String, String> getValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return errors;
    }

    // ✅ Tạo nhân viên mới
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody EmployeeCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(getValidationErrors(bindingResult));
        }
        try {
            Employee employee = saveService.save(request);
            return new ResponseEntity<>(employeeMapper.toResponse(employee), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Cập nhật nhân viên
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id,
                                    @Valid @RequestBody EmployeeUpdateRequest request,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(getValidationErrors(bindingResult));
        }
        try {
            Employee employee = updateService.update(id, request);
            return ResponseEntity.ok(employeeMapper.toResponse(employee));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ❌ Xóa cứng - không dùng nữa, thay bằng xóa mềm
    // @DeleteMapping("/{id}")

    // ✅ Xóa mềm nhân viên (chuyển user.status = DELETED)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Integer id) {
        try {
            updateService.softDeleteEmployee(id);
            return ResponseEntity.ok(Map.of("message", "Đã chuyển nhân viên sang trạng thái đã xóa"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Khôi phục nhân viên đã xóa
    @PatchMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Integer id) {
        try {
            updateService.restoreEmployee(id);
            return ResponseEntity.ok(Map.of("message", "Khôi phục nhân viên thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Khóa / Mở khóa tài khoản nhân viên
    @PatchMapping("/{id}/toggle-lock")
    public ResponseEntity<?> toggleLock(@PathVariable Integer id) {
        try {
            updateService.toggleLockEmployee(id);
            return ResponseEntity.ok(Map.of("message", "Thay đổi trạng thái tài khoản thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Lấy chi tiết nhân viên
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        try {
            Employee employee = timKiemService.findById(id);
            return ResponseEntity.ok(employeeMapper.toResponse(employee));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Lấy tất cả nhân viên
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        List<EmployeeResponse> responses = timKiemService.findAll().stream()
                .map(employeeMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ✅ Tìm kiếm nhân viên có phân trang
    @GetMapping("/search")
    public ResponseEntity<Page<EmployeeResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "employeeId") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<Employee> employeePage = pageService.searchEmployees(keyword, status, page, size, sortBy, direction);
        Page<EmployeeResponse> responsePage = employeePage.map(employeeMapper::toResponse);
        return ResponseEntity.ok(responsePage);
    }

    // ✅ Thống kê nhân viên
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalEmployees", timKiemService.countTotalEmployees());
        stats.put("activeEmployees", timKiemService.countActiveEmployees());
        stats.put("lockedEmployees", timKiemService.countByUserStatus(com.example.backend_fashionstore.enums.UserStatus.BANNED));
        stats.put("deletedEmployees", timKiemService.countByUserStatus(com.example.backend_fashionstore.enums.UserStatus.DELETED));
        return ResponseEntity.ok(stats);
    }

    // ✅ Lấy danh sách roles
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles(){
        return ResponseEntity.ok(roleService.getAllRoles());
    }
}