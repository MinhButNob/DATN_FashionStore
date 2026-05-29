package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.auth.dto.*;
import com.example.backend_fashionstore.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder;

    public CustomerService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Page<CustomerDTO> getAllCustomers(String keyword, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Customer> countRoot = countQuery.from(Customer.class);
        countQuery.select(cb.count(countRoot));

        CriteriaQuery<Customer> query = cb.createQuery(Customer.class);
        Root<Customer> root = query.from(Customer.class);
        root.fetch("user", JoinType.INNER);
        query.select(root);

        List<Predicate> predicates = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("customerName")), likePattern),
                    cb.like(root.get("phone"), likePattern),
                    cb.like(cb.lower(root.get("user").get("email")), likePattern)
            ));
        }

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
            countQuery.where(predicates.toArray(new Predicate[0]));
        }

        TypedQuery<Customer> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<Customer> customers = typedQuery.getResultList();

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(customers.stream().map(this::convertToDTO).toList(), pageable, total);
    }

    public CustomerDTO getCustomerById(Integer id) {
        Customer customer = entityManager.find(Customer.class, id);
        if (customer == null) throw new RuntimeException("Không tìm thấy khách hàng");
        return convertToDTO(customer);
    }

    @Transactional
    public CustomerDTO createCustomer(CustomerCreateDTO dto) {
        TypedQuery<User> emailQuery = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", User.class);
        emailQuery.setParameter("email", dto.getEmail());
        if (!emailQuery.getResultList().isEmpty()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(dto.getEmail().split("@")[0]);
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setProvider("LOCAL");
        user.setIsActive(true);
        entityManager.persist(user);

        Role customerRole = entityManager.find(Role.class, 3);
        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getUserId(), customerRole.getRoleId()));
        userRole.setUser(user);
        userRole.setRole(customerRole);
        entityManager.persist(userRole);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCustomerName(dto.getCustomerName());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setIsLocked(false);
        entityManager.persist(customer);

        return convertToDTO(customer);
    }

    @Transactional
    public CustomerDTO updateCustomer(Integer id, CustomerUpdateDTO dto) {
        Customer customer = entityManager.find(Customer.class, id);
        if (customer == null) throw new RuntimeException("Không tìm thấy khách hàng");

        customer.setCustomerName(dto.getCustomerName());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        entityManager.merge(customer);

        return convertToDTO(customer);
    }

    @Transactional
    public void deleteCustomer(Integer id) {
        Customer customer = entityManager.find(Customer.class, id);
        if (customer == null) throw new RuntimeException("Không tìm thấy khách hàng");
        User user = customer.getUser();
        user.setIsActive(false);
        entityManager.merge(user);
    }

    @Transactional
    public void toggleLockCustomer(Integer id) {
        Customer customer = entityManager.find(Customer.class, id);
        if (customer == null) throw new RuntimeException("Không tìm thấy khách hàng");

        // Xử lý trường hợp isLocked bị null trong database
        boolean currentLockStatus = customer.getIsLocked() != null ? customer.getIsLocked() : false;
        customer.setIsLocked(!currentLockStatus);
        entityManager.merge(customer);
    }

    public StatisticsDTO getStatistics() {
        long total = 0;
        long locked = 0;
        long categories = 0;

        try {
            total = entityManager.createQuery("SELECT COUNT(c) FROM Customer c", Long.class).getSingleResult();
        } catch (Exception e) {
            total = 0;
        }

        try {
            locked = entityManager.createQuery("SELECT COUNT(c) FROM Customer c WHERE c.isLocked = true", Long.class).getSingleResult();
        } catch (Exception e) {
            locked = 0;
        }

        //kco bang product
        categories = 0;

        return new StatisticsDTO(total, total - locked, locked, categories);
    }

    private CustomerDTO convertToDTO(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(c.getCustomerId());
        dto.setUserId(c.getUser().getUserId());
        dto.setEmail(c.getUser().getEmail());
        dto.setUsername(c.getUser().getUsername());
        dto.setCustomerName(c.getCustomerName());
        dto.setPhone(c.getPhone());
        dto.setAddress(c.getAddress());
        dto.setIsLocked(c.getIsLocked());
        dto.setIsActive(c.getUser().getIsActive());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}