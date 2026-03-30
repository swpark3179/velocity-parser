import com.velocityparser.VelocitySimulator;
import com.velocityparser.TestTemplates;
import java.util.HashMap;
import java.util.Map;

public class TestVEngine {
    public static void main(String[] args) {
        VelocitySimulator sim = new VelocitySimulator();
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "PENDING");
        System.out.println("Result: " + sim.simulate(TestTemplates.ORDER_QUERY, vars));
    }
}
