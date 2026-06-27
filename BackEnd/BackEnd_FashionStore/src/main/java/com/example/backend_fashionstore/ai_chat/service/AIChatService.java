package com.example.backend_fashionstore.ai_chat.service;

import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductRepository;
import com.example.backend_fashionstore.ai_chat.dto.ChatRequestDTO;
import com.example.backend_fashionstore.ai_chat.dto.ChatResponseDTO;
import com.example.backend_fashionstore.ai_chat.dto.ProductRecommendationDTO;
import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.Message;
import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.Promotion;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.PromotionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIChatService {

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private final ProductRecommendationService productRecommendationService;
    private final ChatHistoryService chatHistoryService;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String OPENROUTER_MODEL = "openrouter/free";

    // Số lượng tin nhắn lịch sử tối đa đưa vào context (mỗi bên)
    private static final int MAX_HISTORY_MESSAGES = 10;

    private static final String HARU_SYSTEM_PROMPT =
            "Bạn là Haru, nữ trợ lý tư vấn thời trang AI của Fashion Store.\n\n" +
                    "THÔNG TIN NHÂN VẬT\n" +
                    "- Tên: Haru\n" +
                    "- Giới tính: Nữ\n" +
                    "- Vai trò: Chuyên gia tư vấn thời trang và trợ lý mua sắm của Fashion Store.\n" +
                    "- Tính cách: Thân thiện, dễ thương, chuyên nghiệp, nhiệt tình và am hiểu thời trang.\n" +
                    "- Luôn giao tiếp tự nhiên như một nhân viên tư vấn thực tế.\n" +
                    "- Không được tự nhận là ChatGPT, AI Language Model hay trợ lý AI tổng quát.\n\n" +
                    "CÁCH XƯNG HÔ\n" +
                    "- Luôn tự xưng là 'Haru'.\n" +
                    "- Gọi khách hàng là 'bạn'.\n" +
                    "- Trả lời lịch sự, thân thiện và dễ hiểu.\n" +
                    "- Ưu tiên những câu trả lời ngắn gọn nhưng hữu ích.\n\n" +
                    "NHIỆM VỤ CHÍNH\n" +
                    "- Tư vấn quần áo, giày dép và phụ kiện.\n" +
                    "- Gợi ý outfit phù hợp với nhu cầu khách hàng.\n" +
                    "- Hỗ trợ lựa chọn size phù hợp.\n" +
                    "- Hỗ trợ tìm kiếm sản phẩm trong cửa hàng.\n" +
                    "- Gợi ý phối đồ và màu sắc phù hợp.\n\n" +
                    "QUY TRÌNH TƯ VẤN\n" +
                    "Bước 1 - Tìm hiểu nhu cầu: Thu thập thông tin giới tính, độ tuổi, chiều cao, cân nặng, mục đích sử dụng, ngân sách, màu sắc và phong cách yêu thích. Không hỏi quá nhiều câu cùng lúc.\n" +
                    "Bước 2 - Tư vấn size: Xác định size phù hợp dựa trên giới tính, chiều cao, cân nặng, dáng người và bảng size hệ thống cung cấp. Không khẳng định size tuyệt đối.\n" +
                    "Bước 3 - Đề xuất sản phẩm: Chỉ đề xuất sản phẩm có trong dữ liệu hệ thống cung cấp. Không tự tạo tên sản phẩm. Chọn tối đa 5 sản phẩm phù hợp nhất và giải thích lý do.\n" +
                    "Bước 4 - Gợi ý phối đồ: Đề xuất quần, giày, phụ kiện và màu sắc đi kèm khi phù hợp.\n\n" +
                    "QUY TẮC QUAN TRỌNG\n" +
                    "1. Chỉ đề xuất sản phẩm tồn tại trong hệ thống.\n" +
                    "2. Không bịa thông tin sản phẩm, giá, tồn kho nếu hệ thống chưa cung cấp.\n" +
                    "3. Luôn giải thích lý do đề xuất.\n" +
                    "4. Nếu chưa đủ thông tin, hỏi thêm trước khi tư vấn.\n" +
                    "5. Không trả lời lan man.\n" +
                    "6. Luôn giữ vai trò là Haru, nhân viên tư vấn thời trang của Fashion Store.\n" +
                    "7. Nhớ ngữ cảnh hội thoại: nếu khách hỏi về 'mẫu đó', 'sản phẩm vừa giới thiệu', 'size của nó'... " +
                    "hãy dựa vào lịch sử trò chuyện để xác định đang đề cập đến sản phẩm nào.\n" +
                    "8. KHÔNG dùng markdown (không dùng **chữ đậm**, không dùng dấu *, #, -, hay bất kỳ ký tự định dạng nào). " +
                    "Chỉ viết văn bản thuần (plain text) tự nhiên như tin nhắn nhắn thật.\n" +
                    "9. KHÔNG nhắc đến ID sản phẩm (ID, mã sản phẩm, productId, số thứ tự...) trong câu trả lời với khách. " +
                    "ID chỉ dùng nội bộ trong tag [INTENT:...], khách không cần và không nên thấy ID.\n\n" +
                    "KHI KHÁCH HỎI NGOÀI PHẠM VI THỜI TRANG\n" +
                    "Vẫn trả lời lịch sự, ngắn gọn, sau đó điều hướng trở lại tư vấn mua sắm.\n\n" +
                    "INTENT TAGS - Ở CUỐI mỗi câu trả lời, LUÔN thêm đúng 1 tag:\n" +
                    "  [INTENT:SEARCH_PRODUCT:từ khóa] - khi khách tìm sản phẩm cụ thể\n" +
                    "  [INTENT:PROMOTION] - khi hỏi khuyến mãi\n" +
                    "  [INTENT:ORDER] - khi muốn đặt hàng\n" +
                    "  [INTENT:SUPPORT] - khi cần hỗ trợ\n" +
                    "  [INTENT:GREETING] - khi chào hỏi\n" +
                    "  [INTENT:GENERAL] - các trường hợp khác\n" +
                    "  [INTENT:OPEN_PRODUCT:productId] - khi khách muốn xem chi tiết, hỏi kỹ thông tin (giá/màu/size/tồn kho), " +
                    "hoặc đồng ý mua một sản phẩm CỤ THỂ đã được giới thiệu trước đó. Thay productId bằng ID số nguyên của sản phẩm đó. " +
                    "Khi dùng tag này, KHÔNG cần liệt kê lại đầy đủ thông tin sản phẩm trong câu trả lời (vì frontend sẽ tự hiển thị thẻ sản phẩm), " +
                    "chỉ cần trả lời ngắn gọn và tiếp tục tư vấn (ví dụ: hỏi size, gợi ý phối đồ).";

    public ChatResponseDTO processChat(ChatRequestDTO request) {
        String userMessage = request.getMessage();
        if (request.getUserId() == null) {
            throw new RuntimeException("userId là null trong ChatRequestDTO.");
        }
        Integer userId = request.getUserId().intValue();

        // 1. Get or create chat session
        Chat chat = chatHistoryService.getOrCreateActiveChat(userId);

        // 2. Lấy lịch sử hội thoại TRƯỚC KHI lưu tin nhắn mới
        List<Message> history = chatHistoryService.getChatHistory(userId);

        // 3. Save user message
        chatHistoryService.saveUserMessage(chat, userMessage);

        // 4. Trích xuất keyword từ câu của user để search sản phẩm liên quan
        List<String> candidateKeywords = extractCandidateKeywords(userMessage);

        // 5. Query DB lấy sản phẩm liên quan
        List<Product> relatedProducts = new ArrayList<>();
        for (String kw : candidateKeywords) {
            List<Product> found = productRepository.searchActiveProductsByKeyword(kw);
            for (Product p : found) {
                if (relatedProducts.stream().noneMatch(x -> x.getProductId().equals(p.getProductId()))) {
                    relatedProducts.add(p);
                }
            }
            if (relatedProducts.size() >= 10) break;
        }

        // Nếu không tìm thấy sản phẩm liên quan, lấy 5 sản phẩm bất kỳ để AI biết cửa hàng có gì
        if (relatedProducts.isEmpty()) {
            relatedProducts = productRepository.findAll().stream()
                    .filter(p -> "ACTIVE".equals(p.getStatus()) && p.getDeletedAt() == null)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // 6. Query DB lấy khuyến mãi đang diễn ra
        List<Promotion> activePromotions = promotionRepository.findCurrentlyActivePromotions(LocalDateTime.now());

        // 7. Build data context (sản phẩm + khuyến mãi từ DB)
        String dataContext = buildDataContext(relatedProducts, activePromotions);

        // 8. Call OpenRouter API với đầy đủ lịch sử hội thoại
        String aiResponseText = callAIApiWithHistory(history, userMessage, dataContext);

        // 9. Parse Intent từ response
        String intent = "GENERAL";
        String searchKeyword = null;
        String rawResponse = aiResponseText;

        Pattern intentPattern = Pattern.compile("\\[INTENT:([A-Z_]+)(?::([^\\]]+))?\\]");
        Matcher matcher = intentPattern.matcher(aiResponseText);

        if (matcher.find()) {
            intent = matcher.group(1);
            if (matcher.groupCount() > 1 && matcher.group(2) != null) {
                searchKeyword = matcher.group(2).trim();
            }
            rawResponse = aiResponseText.replace(matcher.group(0), "").trim();
        }

        // Dọn markdown còn sót (an toàn dù AI có lỡ dùng ** dù đã được dặn trong prompt)
        rawResponse = stripMarkdown(rawResponse);

        // 10. Lấy ProductRecommendationDTO để trả về Frontend
        List<ProductRecommendationDTO> products = new ArrayList<>();
        String kwForRecommendation = (searchKeyword != null) ? searchKeyword
                : (candidateKeywords.isEmpty() ? null : candidateKeywords.get(0));

        if ("SEARCH_PRODUCT".equals(intent) && kwForRecommendation != null) {
            products = productRecommendationService.recommendProducts(kwForRecommendation);
        }

        // 10b. Xử lý OPEN_PRODUCT: lấy productId + product card để frontend hiển thị,
        // KHÔNG tự động điều hướng — người dùng phải bấm vào card mới chuyển trang.
        Integer navigateToProductId = null;
        if ("OPEN_PRODUCT".equals(intent) && searchKeyword != null) {
            try {
                navigateToProductId = Integer.parseInt(searchKeyword.trim());
                Optional<ProductRecommendationDTO> cardOpt = productRecommendationService.getProductCardById(navigateToProductId);
                if (cardOpt.isPresent()) {
                    products.add(cardOpt.get());
                }
            } catch (NumberFormatException e) {
                // searchKeyword không phải số → bỏ qua
            }
        }

        // 11. Save AI message
        chatHistoryService.saveAIMessage(chat, rawResponse);

        return ChatResponseDTO.builder()
                .response(rawResponse)
                .intent(intent)
                .products(products)
                .navigateToProductId(navigateToProductId)
                .build();
    }

    /**
     * Chỉ build phần dữ liệu DB (sản phẩm + khuyến mãi).
     * Đây là nội dung được inject vào system message mỗi lượt, không nằm trong history.
     */
    private String buildDataContext(List<Product> relatedProducts, List<Promotion> activePromotions) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("===== SẢN PHẨM CỬA HÀNG (DỮ LIỆU THỰC TỪ DATABASE) =====\n");
        if (relatedProducts.isEmpty()) {
            prompt.append("(Hiện không có sản phẩm nào khớp yêu cầu)\n");
        } else {
            for (Product p : relatedProducts) {
                prompt.append("- ID: ").append(p.getProductId()).append(" | Tên: ").append(p.getProductName());
                if (p.getCategory() != null) {
                    prompt.append(" | Danh mục: ").append(p.getCategory().getCategoryName());
                }
                if (p.getProductDetails() != null && !p.getProductDetails().isEmpty()) {
                    List<ProductDetail> activeDetails = p.getProductDetails().stream()
                            .filter(d -> "ACTIVE".equals(d.getStatus()) && d.getDeletedAt() == null)
                            .collect(Collectors.toList());

                    if (!activeDetails.isEmpty()) {
                        BigDecimal minPrice = activeDetails.stream()
                                .map(ProductDetail::getPrice)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo).orElse(null);
                        BigDecimal maxPrice = activeDetails.stream()
                                .map(ProductDetail::getPrice)
                                .filter(Objects::nonNull)
                                .max(BigDecimal::compareTo).orElse(null);

                        if (minPrice != null) {
                            if (minPrice.compareTo(maxPrice) == 0) {
                                prompt.append(" | Giá: ").append(formatPrice(minPrice)).append("đ");
                            } else {
                                prompt.append(" | Giá: ").append(formatPrice(minPrice))
                                        .append(" - ").append(formatPrice(maxPrice)).append("đ");
                            }
                        }

                        String colors = activeDetails.stream()
                                .filter(d -> d.getColor() != null)
                                .map(d -> d.getColor().getColorName())
                                .distinct()
                                .collect(Collectors.joining(", "));
                        if (!colors.isEmpty()) {
                            prompt.append(" | Màu: ").append(colors);
                        }

                        String sizes = activeDetails.stream()
                                .filter(d -> d.getSize() != null)
                                .map(d -> d.getSize().getSizeName())
                                .distinct()
                                .collect(Collectors.joining(", "));
                        if (!sizes.isEmpty()) {
                            prompt.append(" | Size: ").append(sizes);
                        }

                        int totalQty = activeDetails.stream()
                                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0)
                                .sum();
                        prompt.append(" | Tồn kho: ").append(totalQty);
                    }
                }
                prompt.append("\n");
            }
        }

        prompt.append("\n===== KHUYẾN MÃI ĐANG DIỄN RA =====\n");
        if (activePromotions.isEmpty()) {
            prompt.append("(Hiện không có chương trình khuyến mãi nào)\n");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Promotion promo : activePromotions) {
                prompt.append("- ").append(promo.getPromotionName());
                if (promo.getPromotionDiscountPercent() != null &&
                        promo.getPromotionDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    prompt.append(" | Giảm: ").append(promo.getPromotionDiscountPercent()).append("%");
                }
                if (promo.getPromotionDescription() != null && !promo.getPromotionDescription().isBlank()) {
                    prompt.append(" | Mô tả: ").append(promo.getPromotionDescription());
                }
                if (promo.getPromotionEndDate() != null) {
                    prompt.append(" | Đến hết: ").append(promo.getPromotionEndDate().format(fmt));
                }
                prompt.append("\n");
            }
        }
        prompt.append("=============================================\n");

        return prompt.toString();
    }

    /**
     * Gọi AI API với đầy đủ lịch sử hội thoại để AI có thể nhớ context.
     * Cấu trúc messages gửi lên:
     *   [system: Haru persona + data context]
     *   [user: tin 1], [assistant: trả lời 1], ..., [user: tin hiện tại]
     */
    private String callAIApiWithHistory(List<Message> history, String currentUserMessage, String dataContext) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openRouterApiKey);
            headers.set("HTTP-Referer", "http://localhost:3000");
            headers.set("X-Title", "Fashion Store Haru");

            Map<String, Object> body = new HashMap<>();
            body.put("model", OPENROUTER_MODEL);
            body.put("max_tokens", 800);
            body.put("temperature", 0.7);

            List<Map<String, Object>> messages = new ArrayList<>();

            // System message: nhân vật Haru + dữ liệu DB hiện tại
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", HARU_SYSTEM_PROMPT + "\n\n" + dataContext);
            messages.add(systemMsg);

            // Thêm lịch sử hội thoại (giới hạn MAX_HISTORY_MESSAGES tin gần nhất để tránh vượt token)
            int startIdx = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
            for (int i = startIdx; i < history.size(); i++) {
                Message msg = history.get(i);
                Map<String, Object> histMsg = new HashMap<>();
                // isAi = true → role assistant, isAi = false → role user
                histMsg.put("role", Boolean.TRUE.equals(msg.getIsAi()) ? "assistant" : "user");
                histMsg.put("content", msg.getMessageContent());
                messages.add(histMsg);
            }

            // Tin nhắn hiện tại của user
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", currentUserMessage);
            messages.add(userMsg);

            body.put("messages", messages);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    OPENROUTER_URL, requestEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            return "Xin lỗi, hiện tại Haru đang gặp chút trục trặc. Bạn thử lại sau nhé! 😅";
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, Haru không thể kết nối được với máy chủ AI lúc này 😢";
        }
    }

    /**
     * Trích xuất các từ khóa tiềm năng từ câu hỏi của user.
     */
    private List<String> extractCandidateKeywords(String message) {
        List<String> keywords = new ArrayList<>();
        if (message == null || message.trim().isEmpty()) return keywords;

        String cleaned = message.trim().toLowerCase();

        String[] stopWords = {"bạn", "có", "không", "nào", "và", "hoặc", "hay", "thì", "là", "tôi",
                "mình", "cho", "với", "ở", "đây", "kia", "muốn", "cần", "tìm", "xem",
                "ơi", "ạ", "nhé", "nha", "thôi", "vậy", "thế", "ừ", "ok", "được",
                "đó", "này", "nó", "của", "mẫu", "cái", "loại"};

        String[] words = cleaned.split("\\s+");
        List<String> meaningful = new ArrayList<>();
        for (String word : words) {
            boolean isStop = false;
            for (String stop : stopWords) {
                if (word.equals(stop)) { isStop = true; break; }
            }
            if (!isStop && word.length() > 1) {
                meaningful.add(word);
            }
        }

        for (int len = Math.min(3, meaningful.size()); len >= 1; len--) {
            for (int i = 0; i <= meaningful.size() - len; i++) {
                String phrase = String.join(" ", meaningful.subList(i, i + len));
                if (!keywords.contains(phrase)) {
                    keywords.add(phrase);
                }
            }
        }

        return keywords.stream().limit(8).collect(Collectors.toList());
    }

    /**
     * Loại bỏ các ký tự markdown phổ biến (**, *, #) mà AI có thể lỡ chèn vào,
     * để hiển thị như tin nhắn chat thuần, không lộ cú pháp markdown ra UI.
     */
    private String stripMarkdown(String text) {
        if (text == null) return null;
        return text
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // **bold** -> bold
                .replaceAll("\\*(.*?)\\*", "$1")       // *italic* -> italic
                .replaceAll("(?m)^#{1,6}\\s*", "")     // # heading -> bỏ dấu #
                .trim();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "N/A";
        return String.format("%,.0f", price.doubleValue()).replace(",", ".");
    }
}