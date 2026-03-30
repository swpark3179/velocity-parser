import com.velocityparser.VelocityBranchSimulator;
import com.velocityparser.model.BranchResult;

public class TestRun {
    public static void main(String[] args) {
        VelocityBranchSimulator sim = new VelocityBranchSimulator();
        String template = "#if($a == 1) A #elseif($b == 2) B #end";
        try {
            System.out.println("Running simulation...");
            sim.simulateAllBranches(template);
            System.out.println("Simulation successful!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
