import com.velocityparser.VelocityVariableExtractor;
import com.velocityparser.TestTemplates;
import java.util.List;

public class TestRunVars {
    public static void main(String[] args) {
        VelocityVariableExtractor extractor = new VelocityVariableExtractor();
        List<String> vars = extractor.extract(TestTemplates.ORDER_QUERY);
        System.out.println("Extracted vars:");
        for(String v : vars) System.out.println(v);
    }
}
