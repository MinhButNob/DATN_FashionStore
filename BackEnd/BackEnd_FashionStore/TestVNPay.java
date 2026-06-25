import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestVNPay {
    public static void main(String[] args) throws Exception {
        String test = "Thanh toan don hang: 123";
        System.out.println("US_ASCII: " + URLEncoder.encode(test, StandardCharsets.US_ASCII.toString()));
        System.out.println("UTF_8: " + URLEncoder.encode(test, StandardCharsets.UTF_8.toString()));
    }
}
