import org.checkerframework.checker.nullness.qual.*;

import java.util.Properties;

public class GetProperty {

    @NonNull Object nno = new Object();

    void m(Properties p) {

        String s = "line.separator";

        nno = System.getProperty("line.separator");
        // :: error: (assignment.type.incompatible)
        nno = System.getProperty(s);
        // :: error: (assignment.type.incompatible)
        nno = System.getProperty("not.a.builtin.property");

        // :: error: (assignment.type.incompatible)
        nno = p.getProperty("line.separator");
        // :: error: (assignment.type.incompatible)
        nno = p.getProperty(s);
        // :: error: (assignment.type.incompatible)
        nno = p.getProperty("not.a.builtin.property");
    }
}
