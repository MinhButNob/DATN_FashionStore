import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestHash {
    public static void main(String[] args) throws Exception {
        String test = "Thanh toan don hang: 123456";
        String encoded = URLEncoder.encode(test, StandardCharsets.UTF_8.toString());
        System.out.println("Encoded: " + encoded);
    }
}
