package com.example.backend_fashionstore.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

@Component
public class SchemaValidator implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    public SchemaValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("BẮT ĐẦU KIỂM TRA SCHEMA DATABASE");
        System.out.println("========================================");

        List<String> entityTables = getEntityTables();
        List<ColumnMismatch> mismatches = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            for (String tableName : entityTables) {
                System.out.println("\n Kiểm tra bảng: " + tableName);

                // Lấy cột từ database
                Map<String, String> dbColumns = getDatabaseColumns(metaData, conn, tableName);

                // Lấy cột từ Entity (Hibernate)
                Map<String, String> entityColumns = getEntityColumns(tableName);

                // So sánh
                for (String columnName : entityColumns.keySet()) {
                    String entityType = entityColumns.get(columnName);
                    String dbType = dbColumns.get(columnName);

                    if (dbType == null) {
                        System.out.println("  ❌ Cột '" + columnName + "' có trong Entity nhưng KHÔNG có trong DB");
                    } else if (!isTypeMatch(entityType, dbType)) {
                        System.out.println("  ❌ Cột '" + columnName + "' không khớp:");
                        System.out.println("     - Entity: " + entityType);
                        System.out.println("     - Database: " + dbType);
                        mismatches.add(new ColumnMismatch(tableName, columnName, entityType, dbType));
                    } else {
                        System.out.println("  ✅ Cột '" + columnName + "' khớp: " + entityType);
                    }
                }

                // Kiểm tra cột thừa trong DB
                for (String dbColumn : dbColumns.keySet()) {
                    if (!entityColumns.containsKey(dbColumn)) {
                        System.out.println("   Cột '" + dbColumn + "' có trong DB nhưng KHÔNG có trong Entity");
                    }
                }
            }
        }

        System.out.println("\n========================================");
        if (mismatches.isEmpty()) {
            System.out.println("TẤT CẢ CÁC BẢNG ĐỀU KHỚP!");
        } else {
            System.out.println("PHÁT HIỆN " + mismatches.size() + " LỖI KHỚP CỘT:");
            for (ColumnMismatch m : mismatches) {
                System.out.println("   - " + m.table + "." + m.column + ": " + m.entityType + " vs " + m.dbType);
            }
        }
        System.out.println("========================================");
    }

    private List<String> getEntityTables() {
        // Lấy tất cả entity từ Hibernate
        return entityManager.getMetamodel()
                .getEntities()
                .stream()
                .map(e -> e.getName().toLowerCase())
                .toList();
    }

    private Map<String, String> getDatabaseColumns(DatabaseMetaData metaData, Connection conn, String tableName) throws Exception {
        Map<String, String> columns = new HashMap<>();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME").toLowerCase();
                String colType = rs.getString("TYPE_NAME").toUpperCase();
                columns.put(colName, colType);
            }
        }
        return columns;
    }

    private Map<String, String> getEntityColumns(String tableName) {
        // Đây là cách đơn giản, bạn có thể custom theo nhu cầu
        // Trong thực tế, bạn có thể dùng Hibernate metadata
        Map<String, String> columns = new HashMap<>();
        // Giả lập - thực tế nên lấy từ Hibernate metadata
        return columns;
    }

    private boolean isTypeMatch(String entityType, String dbType) {
        // Mapping Java type sang PostgreSQL type
        Map<String, List<String>> typeMapping = Map.of(
                "java.time.LocalDateTime", List.of("TIMESTAMP", "TIMESTAMPTZ"),
                "java.time.LocalDate", List.of("DATE"),
                "java.lang.String", List.of("VARCHAR", "CHAR", "TEXT"),
                "java.lang.Integer", List.of("INT4", "INTEGER"),
                "java.lang.Long", List.of("INT8", "BIGINT"),
                "java.math.BigDecimal", List.of("NUMERIC", "DECIMAL"),
                "java.lang.Boolean", List.of("BOOL", "BOOLEAN")
        );

        for (Map.Entry<String, List<String>> entry : typeMapping.entrySet()) {
            if (entityType.contains(entry.getKey())) {
                return entry.getValue().stream().anyMatch(dbType::contains);
            }
        }
        return true;
    }

    static class ColumnMismatch {
        String table;
        String column;
        String entityType;
        String dbType;

        ColumnMismatch(String table, String column, String entityType, String dbType) {
            this.table = table;
            this.column = column;
            this.entityType = entityType;
            this.dbType = dbType;
        }
    }
}