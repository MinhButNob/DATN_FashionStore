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
import com.example.backend_fashionstore.entity.Voucher;
import com.example.backend_fashionstore.entity.Category;
import com.example.backend_fashionstore.entity.Color;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.PromotionRepository;
import com.example.backend_fashionstore.QuanLyDanhMuc.repository.CategoryRepository;
import com.example.backend_fashionstore.QuanLyMauSac.repository.ColorRepository;
import com.example.backend_fashionstore.QuanLySize.repository.SizeRepository;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
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

    //    @Value("${openrouter.api.key}")
    @Value("${groq.api.key}")
    private String openRouterApiKey;

    private final ProductRecommendationService productRecommendationService;
    private final ChatHistoryService chatHistoryService;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final VoucherRepository voucherRepository;
    private final SizeGuideService sizeGuideService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    //  Link truy cap a.i model
    private static final String OPENROUTER_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String OPENROUTER_MODEL = "llama-3.3-70b-versatile";

//    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
//    private static final String OPENROUTER_MODEL = "openrouter/free";

//    // Đổi sang model cụ thể không có thinking/reasoning để tránh lộ nội dung suy luận nội tâm ra UI
//    private static final String OPENROUTER_MODEL = "mistralai/mistral-7b-instruct:free";

    // Số lượng tin nhắn lịch sử tối đa đưa vào context (mỗi bên)
    private static final int MAX_HISTORY_MESSAGES = 10;

    // Ngưỡng tồn kho được coi là "sắp hết hàng" để Haru chủ động nhắc khách đặt sớm
    private static final int LOW_STOCK_THRESHOLD = 5;

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
                    "QUY TẮC VỀ GIỚI TÍNH\n" +
                    "- Luôn nhớ giới tính của khách hàng (được cung cấp trong phần THÔNG TIN KHÁCH HÀNG).\n" +
                    "- Nếu giới tính là CHƯA BIẾT, PHẢI HỎI KHÁCH 'Bạn là nam hay nữ ạ?' trước khi tư vấn sản phẩm.\n" +
                    "- Chỉ đề xuất sản phẩm phù hợp với giới tính của khách.\n\n" +
                    "QUY TẮC TƯ VẤN SIZE THÔNG MINH\n" +
                    "- Bạn được cung cấp kết quả phân tích size chuẩn từ hệ thống trong mục HƯỚNG DẪN CHỌN SIZE.\n" +
                    "- LUÔN LUÔN phân tích dựa trên kết quả gợi ý đó. Nếu chiều cao khớp size A nhưng cân nặng khớp size B, BẠN PHẢI GIẢI THÍCH RÕ RÀNG ưu nhược điểm của từng size và đưa ra gợi ý cuối cùng như hệ thống đề xuất.\n" +
                    "- Tuyệt đối KHÔNG TỰ SUY DIỄN bảng size trái ngược với thông tin được cung cấp.\n" +
                    "- LUÔN giải thích lý do: 'Vì bạn cao X (size A), nặng Y (size B), nên Haru chọn size Z vì...'\n\n" +
                    "VÍ DỤ TƯ VẤN TỐT\n" +
                    "Khách: Tôi là nam, cao 1m6, nặng 61kg, tư vấn áo sơ mi\n" +
                    "Haru: 📏 Với nam, cao 160cm, nặng 61kg:\n" +
                    "🔍 Phân tích:\n" +
                    "- Size S: 155-165cm, 50-60kg → HỢP CHIỀU CAO (160cm), HƠI NẶNG (61kg > 60kg)\n" +
                    "- Size M: 165-175cm, 58-70kg → HỢP CÂN NẶNG (61kg), HƠI THẤP (160cm < 165cm)\n\n" +
                    "🎯 Haru gợi ý: SIZE M\n\n" +
                    "💡 Lý do: Bạn nặng 61kg nên size S có thể bị chật ở vai và ngực. Size M sẽ thoải mái hơn, dù có thể hơi dài tay một chút.\n" +
                    "📌 Lời khuyên: Nếu bạn thích mặc ôm, thử size S. Nếu thích thoải mái, chọn size M.\n\n" +
                    "QUY TẮC TƯ VẤN THEO TỒN KHO\n" +
                    "- Mỗi sản phẩm trong danh sách hệ thống cung cấp đều có thông tin 'Tồn kho' và 'Trạng thái kho'.\n" +
                    "- TUYỆT ĐỐI không giới thiệu hoặc mời khách mua sản phẩm có trạng thái 'HẾT HÀNG' (tồn kho = 0). Nếu khách hỏi đúng sản phẩm đó, hãy báo hết hàng và chủ động gợi ý sản phẩm tương tự còn hàng trong danh sách.\n" +
                    "- Với sản phẩm có trạng thái 'SẮP HẾT HÀNG' (tồn kho thấp), khi giới thiệu hãy khéo léo nhắc khách rằng sản phẩm này sắp hết hàng nên đặt sớm, tạo cảm giác cấp bách nhưng không nói dối hay phóng đại.\n" +
                    "- Khi đề xuất danh sách sản phẩm (Bước 3), ƯU TIÊN sản phẩm còn nhiều hàng hơn nếu các sản phẩm có mức độ phù hợp tương đương nhau.\n" +
                    "- KHÔNG tự đọc to số tồn kho cụ thể (ví dụ 'còn 7 cái') trừ khi khách hỏi trực tiếp số lượng còn lại; bình thường chỉ cần nói chung là 'còn hàng' / 'sắp hết hàng' / 'hết hàng'.\n\n" +
                    "QUY TRÌNH TƯ VẤN\n" +
                    "Bước 1 - Tìm hiểu nhu cầu: Thu thập thông tin giới tính, độ tuổi, chiều cao, cân nặng, mục đích sử dụng. Không hỏi quá nhiều câu cùng lúc.\n" +
                    "Bước 2 - Tư vấn size: Dựa trên phân tích từ hệ thống, đưa ra lời khuyên. Không khẳng định tuyệt đối.\n" +
                    "Bước 3 - Đề xuất sản phẩm: Chỉ đề xuất sản phẩm có trong dữ liệu hệ thống cung cấp. Chọn tối đa 5 sản phẩm phù hợp nhất và giải thích lý do.\n" +
                    "Bước 4 - Gợi ý phối đồ: Đề xuất quần, giày, phụ kiện khi phù hợp.\n\n" +
                    "QUY TẮC QUAN TRỌNG\n" +
                    "1. Chỉ đề xuất sản phẩm tồn tại trong hệ thống.\n" +
                    "2. Không bịa thông tin sản phẩm, giá, tồn kho nếu hệ thống chưa cung cấp.\n" +
                    "3. Luôn giải thích lý do đề xuất.\n" +
                    "4. KHÔNG dùng markdown (không dùng **chữ đậm**, không dùng dấu *, #, -, hay bất kỳ ký tự định dạng nào). Chỉ viết văn bản thuần (plain text).\n" +
                    "5. KHÔNG nhắc đến ID sản phẩm trong câu trả lời với khách. ID chỉ dùng nội bộ trong tag.\n" +
                    "6. Khi khách hỏi về sản phẩm, ưu tiên tra cứu trong danh sách sản phẩm được cung cấp.\n" +
                    "7. Nếu thông tin không có trong database, nói rõ 'Hiện tại Haru chưa có thông tin này'.\n\n" +
                    "- Khi khách hỏi về đổi trả, hoàn hàng, hoàn tiền, bảo hành, khiếu nại, hoặc bất kỳ câu hỏi nào Haru không đủ thông tin/thẩm quyền để trả lời chắc chắn, hãy trả lời ngắn gọn, thể hiện sự thấu hiểu, sau đó CHỦ ĐỘNG mời khách nhắn tin trực tiếp với nhân viên tư vấn.\n" +
                    "- Câu mẫu tham khảo: 'Về vấn đề này Haru xin phép chuyển bạn đến nhân viên tư vấn để được hỗ trợ chi tiết và nhanh hơn nhé, bạn nhắn tin trực tiếp với nhân viên ở khung chat bên dưới giúp Haru nha!'\n" +
                    "- TUYỆT ĐỐI KHÔNG tự bịa chính sách đổi trả/hoàn hàng cụ thể (thời hạn, điều kiện, phí) nếu dữ liệu hệ thống không cung cấp.\n\n" +
                    "KHI KHÁCH HỎI NGOÀI PHẠM VI THỜI TRANG\n" +
                    "Vẫn trả lời lịch sự, ngắn gọn, sau đó điều hướng trở lại tư vấn mua sắm.\n\n" +
                    "INTENT TAGS - Ở CUỐI mỗi câu trả lời, LUÔN thêm đúng 1 tag:\n" +
                    "  [INTENT:SEARCH_PRODUCT:từ khóa] - khi khách tìm sản phẩm cụ thể\n" +
                    "  [INTENT:PROMOTION] - khi hỏi khuyến mãi\n" +
                    "  [INTENT:ORDER] - khi muốn đặt hàng\n" +
                    "  [INTENT:SUPPORT] - khi cần hỗ trợ\n" +
                    "  [INTENT:SUPPORT] - khi cần hỗ trợ, khi khách hỏi hoàn hàng/đổi trả/khiếu nại, hoặc câu hỏi khó cần chuyển nhân viên tư vấn\n" +
                    "  [INTENT:GREETING] - khi chào hỏi\n" +
                    "  [INTENT:GENERAL] - các trường hợp khác\n" +
                    "  [INTENT:OPEN_PRODUCT:productId] - khi khách muốn xem chi tiết hoặc đồng ý mua một sản phẩm CỤ THỂ đã được giới thiệu.";

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

        // 3b. Trích xuất thông tin khách hàng từ tin nhắn
        extractUserInfoFromMessage(userId, userMessage);

        // 3c. Lấy context khách hàng (đã lưu)
        ChatHistoryService.UserContext userContext = chatHistoryService.getUserContext(userId);

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

        // Nếu không tìm thấy sản phẩm liên quan, lấy tối đa 50 sản phẩm active để AI biết cửa hàng có gì
        if (relatedProducts.isEmpty()) {
            relatedProducts = productRepository.findAllActiveProducts().stream()
                    .limit(50)
                    .collect(Collectors.toList());
        }

        // 6. Query DB lấy khuyến mãi đang diễn ra
        List<Promotion> activePromotions = promotionRepository.findAllActivePromotions(LocalDateTime.now());

        // Lấy voucher customer chưa sử dụng
        List<Voucher> availableVouchers = voucherRepository.findUnusedVouchersByCustomer(userId);

        // Lấy danh mục, màu sắc (có thể caching ở thực tế nhưng ở đây query trực tiếp)
        List<Category> categories = categoryRepository.findAll();
        List<Color> colors = colorRepository.findAll();

        // 7. Build data context (sản phẩm + khuyến mãi + voucher + size guide)
        String dataContext = buildDataContext(relatedProducts, activePromotions, availableVouchers, categories, colors, userContext);

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
    private String buildDataContext(List<Product> relatedProducts, List<Promotion> activePromotions, List<Voucher> availableVouchers, List<Category> categories, List<Color> colors, ChatHistoryService.UserContext userContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("===== THÔNG TIN KHÁCH HÀNG =====\n");
        if (userContext.getGender() == null || userContext.getGender().equalsIgnoreCase("UNKNOWN")) {
            prompt.append("- Giới tính: CHƯA BIẾT (Hãy hỏi khách 'Bạn là nam hay nữ ạ?')\n");
        } else {
            prompt.append("- Giới tính: ").append(userContext.getGender().equalsIgnoreCase("MALE") ? "Nam" : "Nữ").append("\n");
        }
        if (userContext.getHeight() != null) {
            prompt.append("- Chiều cao: ").append(userContext.getHeight().intValue()).append("cm\n");
        } else {
            prompt.append("- Chiều cao: CHƯA BIẾT\n");
        }
        if (userContext.getWeight() != null) {
            prompt.append("- Cân nặng: ").append(userContext.getWeight().intValue()).append("kg\n");
        } else {
            prompt.append("- Cân nặng: CHƯA BIẾT\n");
        }
        prompt.append("\n");

        prompt.append("===== HƯỚNG DẪN CHỌN SIZE TỪ HỆ THỐNG =====\n");
        prompt.append(sizeGuideService.getSizeGuide(userContext.getGender()));
        prompt.append("\n");

        prompt.append("===== DANH MỤC SẢN PHẨM =====\n");
        if (categories.isEmpty()) prompt.append("(Trống)\n");
        else {
            String cats = categories.stream().map(Category::getCategoryName).collect(Collectors.joining(", "));
            prompt.append(cats).append("\n");
        }
        prompt.append("\n");

        prompt.append("===== MÀU SẮC CÓ SẴN =====\n");
        if (colors.isEmpty()) prompt.append("(Trống)\n");
        else {
            String cols = colors.stream().map(Color::getColorName).collect(Collectors.joining(", "));
            prompt.append(cols).append("\n");
        }
        prompt.append("\n");

        prompt.append("===== SẢN PHẨM CỬA HÀNG (TỐI ĐA 50 SẢN PHẨM) =====\n");
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

                        String colorsStr = activeDetails.stream()
                                .filter(d -> d.getColor() != null)
                                .map(d -> d.getColor().getColorName())
                                .distinct()
                                .collect(Collectors.joining(", "));
                        if (!colorsStr.isEmpty()) {
                            prompt.append(" | Màu: ").append(colorsStr);
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
                        String stockStatus = totalQty <= 0 ? "HẾT HÀNG"
                                : (totalQty <= LOW_STOCK_THRESHOLD ? "SẮP HẾT HÀNG" : "CÒN NHIỀU HÀNG");
                        prompt.append(" | Tồn kho: ").append(totalQty)
                                .append(" | Trạng thái kho: ").append(stockStatus);
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

        prompt.append("\n===== VOUCHER KHÁCH HÀNG CÓ THỂ SỬ DỤNG =====\n");
        if (availableVouchers == null || availableVouchers.isEmpty()) {
            prompt.append("(Khách hàng chưa có voucher nào chưa sử dụng)\n");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Voucher voucher : availableVouchers) {
                prompt.append("- Mã: ").append(voucher.getVoucherCode());
                if (voucher.getVoucherDiscountAmount() != null && voucher.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    prompt.append(" | Giảm giá: ").append(formatPrice(voucher.getVoucherDiscountAmount())).append("đ");
                } else if (voucher.getVoucherDiscountPercent() != null && voucher.getVoucherDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    prompt.append(" | Giảm giá: ").append(voucher.getVoucherDiscountPercent()).append("%");
                }
                if (voucher.getVoucherMinOrderValue() != null && voucher.getVoucherMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
                    prompt.append(" | Đơn tối thiểu: ").append(formatPrice(voucher.getVoucherMinOrderValue())).append("đ");
                }
                if (voucher.getVoucherEndDate() != null) {
                    prompt.append(" | Hạn SD: ").append(voucher.getVoucherEndDate().format(fmt));
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
                        String raw = (String) message.get("content");
                        // FIX: Lọc phần thinking/reasoning nội tâm mà một số model (DeepSeek-R1, Qwen-thinking...)
                        // trả về trong content. Nếu không lọc, toàn bộ quá trình suy luận sẽ hiện ra UI.
                        return stripThinking(raw);
                    }
                }
            }
            return "Xin lỗi, hiện tại Haru đang gặp chút trục trặc. Bạn thử lại sau nhé! 😅";
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, Haru không thể kết nối được với máy chủ AI lúc này 😢";
        }
    }

    private void extractUserInfoFromMessage(Integer userId, String message) {
        if (message == null || message.trim().isEmpty()) return;
        String lowerMsg = message.toLowerCase();

        String gender = null;
        Double height = null;
        Double weight = null;

        // Trích xuất giới tính
        if (lowerMsg.matches(".*\\b(nam|trai|con trai)\\b.*")) {
            gender = "MALE";
        } else if (lowerMsg.matches(".*\\b(nữ|gái|con gái)\\b.*")) {
            gender = "FEMALE";
        }

        // Trích xuất chiều cao
        // Các pattern: cao 1m6, 1m65, 1m70, 165cm, 165 cm, chiều cao 160
        Pattern heightPattern1 = Pattern.compile("1m(\\d{1,2})"); // 1m6, 1m65
        Matcher mHeight1 = heightPattern1.matcher(lowerMsg);
        if (mHeight1.find()) {
            String cmStr = mHeight1.group(1);
            if (cmStr.length() == 1) cmStr += "0"; // 1m6 -> 160
            height = 100.0 + Double.parseDouble(cmStr);
        } else {
            Pattern heightPattern2 = Pattern.compile("(\\d{3})\\s*cm"); // 165cm, 165 cm
            Matcher mHeight2 = heightPattern2.matcher(lowerMsg);
            if (mHeight2.find()) {
                height = Double.parseDouble(mHeight2.group(1));
            } else {
                Pattern heightPattern3 = Pattern.compile("cao\\s+(\\d{3})"); // cao 160
                Matcher mHeight3 = heightPattern3.matcher(lowerMsg);
                if (mHeight3.find()) {
                    height = Double.parseDouble(mHeight3.group(1));
                }
            }
        }

        // Trích xuất cân nặng
        // Các pattern: nặng 61kg, 61 kg, 61kg, 61 kí, 61 cân
        Pattern weightPattern = Pattern.compile("(\\d{2,3})\\s*(kg|kí|cân)");
        Matcher mWeight = weightPattern.matcher(lowerMsg);
        if (mWeight.find()) {
            weight = Double.parseDouble(mWeight.group(1));
        } else {
            Pattern weightPattern2 = Pattern.compile("nặng\\s+(\\d{2,3})");
            Matcher mWeight2 = weightPattern2.matcher(lowerMsg);
            if (mWeight2.find()) {
                weight = Double.parseDouble(mWeight2.group(1));
            }
        }

        chatHistoryService.updateUserContext(userId, gender, height, weight);
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
     * Loại bỏ phần "thinking" nội tâm mà một số model AI (DeepSeek-R1, Qwen-thinking, v.v.)
     * trả về bên trong thẻ <think>...</think> hoặc <thinking>...</thinking>.
     * Nếu không lọc, toàn bộ quá trình suy luận sẽ bị hiển thị thẳng ra giao diện chat.
     */
    private String stripThinking(String text) {
        if (text == null) return null;
        // Xử lý <think>...</think>
        text = text.replaceAll("(?s)<think>.*?</think>", "").trim();
        // Xử lý <thinking>...</thinking>
        text = text.replaceAll("(?s)<thinking>.*?</thinking>", "").trim();
        return text;
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