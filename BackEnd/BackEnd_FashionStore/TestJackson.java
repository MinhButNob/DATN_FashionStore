import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.PosSessionResponse;

public class TestJackson {
    public static void main(String[] args) throws Exception {
        PosSessionResponse res = new PosSessionResponse();
        res.setPosSessionId(1);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(res));
    }
}
