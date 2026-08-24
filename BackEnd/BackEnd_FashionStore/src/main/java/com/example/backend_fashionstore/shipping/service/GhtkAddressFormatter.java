package com.example.backend_fashionstore.shipping.service;

import java.util.Locale;

/** Chuẩn hóa các trường địa chỉ theo format text mà GHTK yêu cầu. */
public final class GhtkAddressFormatter {
    private GhtkAddressFormatter() {
    }

    public static FormattedAddress format(String province, String district, String ward) {
        return new FormattedAddress(
                formatProvince(province),
                clean(district),
                formatWard(ward)
        );
    }

    public static String formatProvince(String value) {
        String province = clean(value);
        if (province.equalsIgnoreCase("Thừa Thiên Huế")) return province;
        return removePrefix(province, "Tỉnh ", "Thành phố ");
    }

    /** GHTK fee API nhận tên ward không kèm Phường/Xã/Thị trấn. */
    public static String formatWard(String value) {
        return removePrefix(clean(value), "Phường ", "Xã ", "Thị trấn ");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String removePrefix(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return value.substring(prefix.length()).trim();
            }
        }
        return value;
    }

    public record FormattedAddress(String province, String district, String ward) {
    }
}
