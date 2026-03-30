import java.lang.reflect.Method;
import org.apache.velocity.runtime.RuntimeInstance;

public class TestMethods {
    public static void main(String[] args) {
        Method[] methods = RuntimeInstance.class.getMethods();
        for (Method m : methods) {
            if (m.getName().equals("parse")) {
                System.out.println(m.toString());
            }
        }
    }
}
