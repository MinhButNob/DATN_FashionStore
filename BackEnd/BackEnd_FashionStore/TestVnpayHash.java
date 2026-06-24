import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

public class TestVnpayHash {
    public static void main(String[] args) throws Exception {
        String secretKey = "L9RA4B5G0JAXB484EKNZJ91NRFMQC8U1";
        String vnp_TmnCode = "0T737TPU";
        String txnRef = "aTMRhSg1Bt"; // as in user's image
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", "1000000"); // 10000 VND
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + txnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", "http://localhost:8080/api/payment/vnpay-return");
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");
        vnp_Params.put("vnp_CreateDate", "20260624134708"); // approx time in image
        vnp_Params.put("vnp_ExpireDate", "20260624140208");

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20");
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()).replace("+", "%20");
                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedName).append('=').append(encodedValue);
            }
        }
        
        String queryUrl = query.toString();
        
        // HMAC SHA512
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        byte[] hmacKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
        hmac512.init(keySpec);
        byte[] result = hmac512.doFinal(hashData.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(2 * result.length);
        for (byte b : result) {
            sb.append(String.format("%02x", b & 0xff));
        }
        String vnp_SecureHash = sb.toString();
        
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?" + queryUrl;
        
        System.out.println(paymentUrl);
    }
}
