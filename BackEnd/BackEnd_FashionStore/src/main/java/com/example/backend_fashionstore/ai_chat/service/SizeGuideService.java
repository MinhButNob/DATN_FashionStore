package com.example.backend_fashionstore.ai_chat.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SizeGuideService {

    @Data
    public static class SizeRule {
        private String size;
        private int minHeight;
        private int maxHeight;
        private int minWeight;
        private int maxWeight;
        private String details;

        public SizeRule(String size, int minHeight, int maxHeight, int minWeight, int maxWeight, String details) {
            this.size = size;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.minWeight = minWeight;
            this.maxWeight = maxWeight;
            this.details = details;
        }
    }

    private static final Map<String, Map<String, List<SizeRule>>> SIZE_RULES = new HashMap<>();

    static {
        // --- BẢNG SIZE NAM (MALE) ---
        Map<String, List<SizeRule>> maleRules = new HashMap<>();
        maleRules.put("Áo sơ mi", Arrays.asList(
                new SizeRule("S", 155, 165, 50, 60, "Vòng ngực 80-86cm"),
                new SizeRule("M", 165, 175, 58, 70, "Vòng ngực 86-92cm"),
                new SizeRule("L", 175, 185, 68, 80, "Vòng ngực 92-98cm"),
                new SizeRule("XL", 185, 195, 75, 90, "Vòng ngực 98-104cm")
        ));
        maleRules.put("Áo thun", Arrays.asList(
                new SizeRule("S", 155, 165, 50, 60, "Vòng ngực 82-88cm"),
                new SizeRule("M", 165, 175, 58, 70, "Vòng ngực 88-94cm"),
                new SizeRule("L", 175, 185, 68, 80, "Vòng ngực 94-100cm"),
                new SizeRule("XL", 185, 195, 75, 90, "Vòng ngực 100-106cm")
        ));
        maleRules.put("Quần tây", Arrays.asList(
                new SizeRule("28", 155, 165, 50, 58, "Eo 71-74cm"),
                new SizeRule("30", 165, 175, 58, 68, "Eo 76-79cm"),
                new SizeRule("32", 170, 180, 65, 75, "Eo 81-84cm"),
                new SizeRule("34", 175, 185, 75, 85, "Eo 86-89cm")
        ));
        maleRules.put("Quần jeans", Arrays.asList(
                new SizeRule("28", 155, 165, 50, 58, "Eo 71-74cm"),
                new SizeRule("30", 165, 175, 58, 68, "Eo 76-79cm"),
                new SizeRule("32", 170, 180, 65, 75, "Eo 81-84cm"),
                new SizeRule("34", 175, 185, 75, 85, "Eo 86-89cm")
        ));
        maleRules.put("Áo khoác", Arrays.asList(
                new SizeRule("S", 155, 165, 50, 60, "Vòng ngực 84-90cm"),
                new SizeRule("M", 165, 175, 58, 70, "Vòng ngực 90-96cm"),
                new SizeRule("L", 175, 185, 68, 80, "Vòng ngực 96-102cm"),
                new SizeRule("XL", 185, 195, 75, 90, "Vòng ngực 102-108cm")
        ));
        maleRules.put("Giày", Arrays.asList(
                new SizeRule("39", 0, 0, 0, 0, "Chiều dài chân 24-24.5cm"),
                new SizeRule("40", 0, 0, 0, 0, "Chiều dài chân 24.5-25cm"),
                new SizeRule("41", 0, 0, 0, 0, "Chiều dài chân 25-25.5cm"),
                new SizeRule("42", 0, 0, 0, 0, "Chiều dài chân 25.5-26cm"),
                new SizeRule("43", 0, 0, 0, 0, "Chiều dài chân 26-26.5cm")
        ));
        SIZE_RULES.put("MALE", maleRules);

        // --- BẢNG SIZE NỮ (FEMALE) ---
        Map<String, List<SizeRule>> femaleRules = new HashMap<>();
        femaleRules.put("Áo sơ mi", Arrays.asList(
                new SizeRule("S", 145, 155, 40, 48, "Vòng ngực 76-82cm"),
                new SizeRule("M", 155, 162, 45, 53, "Vòng ngực 80-86cm"),
                new SizeRule("L", 160, 168, 50, 58, "Vòng ngực 84-90cm"),
                new SizeRule("XL", 165, 175, 55, 65, "Vòng ngực 88-94cm")
        ));
        femaleRules.put("Áo thun", Arrays.asList(
                new SizeRule("S", 145, 155, 40, 48, "Vòng ngực 78-84cm"),
                new SizeRule("M", 155, 162, 45, 53, "Vòng ngực 82-88cm"),
                new SizeRule("L", 160, 168, 50, 58, "Vòng ngực 86-92cm"),
                new SizeRule("XL", 165, 175, 55, 65, "Vòng ngực 90-96cm")
        ));
        femaleRules.put("Đầm", Arrays.asList(
                new SizeRule("S", 150, 158, 42, 50, "Eo 60-64cm, Hông 84-88cm"),
                new SizeRule("M", 158, 165, 48, 55, "Eo 64-68cm, Hông 88-92cm"),
                new SizeRule("L", 165, 172, 54, 62, "Eo 68-72cm, Hông 92-96cm"),
                new SizeRule("XL", 170, 178, 60, 70, "Eo 72-76cm, Hông 96-100cm")
        ));
        femaleRules.put("Váy", Arrays.asList(
                new SizeRule("S", 150, 158, 42, 50, "Eo 60-64cm, Hông 84-88cm"),
                new SizeRule("M", 158, 165, 48, 55, "Eo 64-68cm, Hông 88-92cm"),
                new SizeRule("L", 165, 172, 54, 62, "Eo 68-72cm, Hông 92-96cm"),
                new SizeRule("XL", 170, 178, 60, 70, "Eo 72-76cm, Hông 96-100cm")
        ));
        femaleRules.put("Chân váy", Arrays.asList(
                new SizeRule("S", 150, 158, 42, 50, "Eo 60-64cm, Hông 84-88cm"),
                new SizeRule("M", 158, 165, 48, 55, "Eo 64-68cm, Hông 88-92cm"),
                new SizeRule("L", 165, 172, 54, 62, "Eo 68-72cm, Hông 92-96cm"),
                new SizeRule("XL", 170, 178, 60, 70, "Eo 72-76cm, Hông 96-100cm")
        ));
        femaleRules.put("Quần", Arrays.asList(
                new SizeRule("S", 150, 158, 42, 50, "Eo 60-64cm, Hông 84-88cm"),
                new SizeRule("M", 158, 165, 48, 55, "Eo 64-68cm, Hông 88-92cm"),
                new SizeRule("L", 165, 172, 54, 62, "Eo 68-72cm, Hông 92-96cm"),
                new SizeRule("XL", 170, 178, 60, 70, "Eo 72-76cm, Hông 96-100cm")
        ));
        femaleRules.put("Giày", Arrays.asList(
                new SizeRule("35", 0, 0, 0, 0, "Chiều dài chân 22-22.5cm"),
                new SizeRule("36", 0, 0, 0, 0, "Chiều dài chân 22.5-23cm"),
                new SizeRule("37", 0, 0, 0, 0, "Chiều dài chân 23-23.5cm"),
                new SizeRule("38", 0, 0, 0, 0, "Chiều dài chân 23.5-24cm"),
                new SizeRule("39", 0, 0, 0, 0, "Chiều dài chân 24-24.5cm")
        ));
        SIZE_RULES.put("FEMALE", femaleRules);
    }

    public String getSizeGuide(String gender) {
        StringBuilder guide = new StringBuilder();
        guide.append("📏 BẢNG HƯỚNG DẪN CHỌN SIZE:\n\n");

        if (gender == null || gender.equalsIgnoreCase("UNKNOWN")) {
            guide.append("Vui lòng xác định giới tính khách hàng để lấy bảng size phù hợp.\n");
            return guide.toString();
        }

        String genderKey = gender.toUpperCase();
        Map<String, List<SizeRule>> rules = SIZE_RULES.get(genderKey);
        
        if (rules == null) {
            return "Không tìm thấy bảng size cho giới tính: " + gender;
        }

        for (Map.Entry<String, List<SizeRule>> entry : rules.entrySet()) {
            guide.append("👔 ").append(entry.getKey()).append(":\n");
            for (SizeRule rule : entry.getValue()) {
                guide.append("  • Size ").append(rule.getSize()).append(": ");
                if (rule.getMinHeight() > 0 && rule.getMaxHeight() > 0) {
                    guide.append(rule.getMinHeight()).append("-").append(rule.getMaxHeight()).append("cm | ");
                }
                if (rule.getMinWeight() > 0 && rule.getMaxWeight() > 0) {
                    guide.append(rule.getMinWeight()).append("-").append(rule.getMaxWeight()).append("kg | ");
                }
                guide.append(rule.getDetails()).append("\n");
            }
            guide.append("\n");
        }

        return guide.toString();
    }

    public String suggestSize(String gender, String category, Double height, Double weight) {
        if (gender == null || gender.equalsIgnoreCase("UNKNOWN")) {
            return "Vui lòng cho Haru biết bạn là nam hay nữ để gợi ý size chuẩn nhất nhé!";
        }

        if (height == null || weight == null) {
            return "Vui lòng cung cấp chiều cao và cân nặng để Haru gợi ý size chuẩn nhất nhé!";
        }

        if (category == null || category.isEmpty()) {
            return "Bạn muốn tư vấn size cho danh mục nào (Áo sơ mi, Quần tây, Đầm...)?";
        }

        String genderKey = gender.toUpperCase();
        Map<String, List<SizeRule>> categoryRules = SIZE_RULES.get(genderKey);
        
        if (categoryRules == null) {
            return "Haru chưa có dữ liệu size cho giới tính này.";
        }

        String matchedCategory = null;
        for (String key : categoryRules.keySet()) {
            if (category.toLowerCase().contains(key.toLowerCase()) || key.toLowerCase().contains(category.toLowerCase())) {
                matchedCategory = key;
                break;
            }
        }

        if (matchedCategory == null) {
            return "Haru chưa có bảng size cho '" + category + "'. Bạn có thể chọn: " + String.join(", ", categoryRules.keySet());
        }

        if (matchedCategory.equalsIgnoreCase("Giày")) {
            return "Với giày dép, Haru cần biết chiều dài bàn chân (cm) để tư vấn chính xác ạ!";
        }

        List<SizeRule> rules = categoryRules.get(matchedCategory);
        
        SizeRule heightMatchSize = null;
        SizeRule weightMatchSize = null;
        SizeRule exactMatchSize = null;

        for (SizeRule rule : rules) {
            boolean matchH = height >= rule.getMinHeight() && height <= rule.getMaxHeight();
            boolean matchW = weight >= rule.getMinWeight() && weight <= rule.getMaxWeight();

            if (matchH && matchW) {
                exactMatchSize = rule;
            } else if (matchH) {
                heightMatchSize = rule;
            } else if (matchW) {
                weightMatchSize = rule;
            }
        }

        String prefix = "📏 Với " + (genderKey.equals("MALE") ? "nam" : "nữ") + ", cao " + height.intValue() + "cm, nặng " + weight.intValue() + "kg:\n\n🔍 Phân tích:\n";

        if (exactMatchSize != null) {
            return prefix + "- Size " + exactMatchSize.getSize() + ": " + exactMatchSize.getMinHeight() + "-" + exactMatchSize.getMaxHeight() + "cm, " + exactMatchSize.getMinWeight() + "-" + exactMatchSize.getMaxWeight() + "kg → HOÀN TOÀN PHÙ HỢP CẢ CHIỀU CAO LẪN CÂN NẶNG\n\n🎯 Haru gợi ý: SIZE " + exactMatchSize.getSize() + "\n\n💡 Lý do: Các số đo của bạn đều nằm chuẩn trong khoảng của size này, mặc sẽ rất vừa vặn!";
        }

        if (heightMatchSize != null && weightMatchSize != null) {
            // Khác size nhau -> ưu tiên cân nặng hay chiều cao tùy thuộc
            // Mặc định, quần áo thường ưu tiên chiều rộng (cân nặng) để mặc vừa, nhưng chiều dài (chiều cao) cũng quan trọng.
            // Nếu quần, ưu tiên bụng/hông (cân nặng). Nếu áo, cũng ưu tiên bề ngang (cân nặng). 
            // Đoạn này ta báo rõ cả 2 size để khách hàng hiểu:
            String reason = "Bạn nặng " + weight.intValue() + "kg nên size " + heightMatchSize.getSize() + " có thể bị chật/rộng. Size " + weightMatchSize.getSize() + " sẽ vừa vặn bề ngang hơn, dù có thể hơi dài/ngắn tay chân một chút.";
            
            return prefix + 
                   "- Size " + heightMatchSize.getSize() + ": " + heightMatchSize.getMinHeight() + "-" + heightMatchSize.getMaxHeight() + "cm, " + heightMatchSize.getMinWeight() + "-" + heightMatchSize.getMaxWeight() + "kg → HỢP CHIỀU CAO, LỆCH CÂN NẶNG\n" +
                   "- Size " + weightMatchSize.getSize() + ": " + weightMatchSize.getMinHeight() + "-" + weightMatchSize.getMaxHeight() + "cm, " + weightMatchSize.getMinWeight() + "-" + weightMatchSize.getMaxWeight() + "kg → HỢP CÂN NẶNG, LỆCH CHIỀU CAO\n\n" +
                   "🎯 Haru gợi ý: SIZE " + weightMatchSize.getSize() + " hoặc " + heightMatchSize.getSize() + "\n\n" +
                   "💡 Lý do: " + reason + "\n\n" +
                   "📌 Lời khuyên: Nếu bạn thích mặc ôm, thử size " + (weight < heightMatchSize.getMinWeight() ? weightMatchSize.getSize() : heightMatchSize.getSize()) + ". Nếu thích thoải mái, chọn size " + (weight > heightMatchSize.getMaxWeight() ? weightMatchSize.getSize() : heightMatchSize.getSize()) + ".";
        }

        if (heightMatchSize != null) {
            return prefix + "- Size " + heightMatchSize.getSize() + ": " + heightMatchSize.getMinHeight() + "-" + heightMatchSize.getMaxHeight() + "cm, " + heightMatchSize.getMinWeight() + "-" + heightMatchSize.getMaxWeight() + "kg → HỢP CHIỀU CAO, nhưng không nằm trong bất kỳ size cân nặng nào.\n\n🎯 Haru gợi ý: SIZE " + heightMatchSize.getSize() + "\n\n💡 Lý do: Chiều cao của bạn hợp size này, nhưng cân nặng hơi lệch ra khỏi tiêu chuẩn. Bạn nên cân nhắc thêm số đo vòng ngực/eo nhé.";
        }

        if (weightMatchSize != null) {
            return prefix + "- Size " + weightMatchSize.getSize() + ": " + weightMatchSize.getMinHeight() + "-" + weightMatchSize.getMaxHeight() + "cm, " + weightMatchSize.getMinWeight() + "-" + weightMatchSize.getMaxWeight() + "kg → HỢP CÂN NẶNG, nhưng không nằm trong bất kỳ size chiều cao nào.\n\n🎯 Haru gợi ý: SIZE " + weightMatchSize.getSize() + "\n\n💡 Lý do: Cân nặng của bạn hợp size này để mặc vừa bề ngang, nhưng chiều dài có thể không như ý. Tốt nhất là dựa theo số đo vòng ngực/eo chi tiết.";
        }

        return "📏 Với số đo " + height.intValue() + "cm - " + weight.intValue() + "kg, Haru thấy bạn nằm ở rìa bảng size. Để tư vấn chính xác nhất, bạn có thể cho Haru biết số đo vòng ngực/eo/hông được không ạ?";
    }
}