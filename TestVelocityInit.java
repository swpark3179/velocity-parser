import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.Template;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapterImpl;
import java.io.StringReader;
import java.util.Properties;

public class TestVelocityInit {
    public static void main(String[] args) throws Exception {
        RuntimeInstance ri = new RuntimeInstance();
        Properties props = new Properties();
        props.setProperty(RuntimeInstance.RUNTIME_LOG_INSTANCE, "");
        ri.init(props);
        
        Template t = new Template();
        t.setName("test");
        t.setRuntimeServices(ri);
        
        String template = "#if($user.name != '') Hello $user.age #end";
        SimpleNode root = ri.parse(new StringReader(template), t);
        
        traverse(root, "Before init");
        
        root.init(new InternalContextAdapterImpl(new VelocityContext()), ri);
        traverse(root, "After init");
    }
    
    static void traverse(Node node, String prefix) {
        if (node instanceof ASTReference) {
            ASTReference ref = (ASTReference) node;
            System.out.println(prefix + " -> rootString: " + ref.getRootString() + ", literal: " + ref.literal());
        }
        for (int i=0; i<node.jjtGetNumChildren(); i++) traverse(node.jjtGetChild(i), prefix);
    }
}
