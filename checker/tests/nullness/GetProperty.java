import java.util.Properties;
import org.checkerframework.checker.nullness.qual.*;

public class GetProperty {

  @NonNull Object nno = new Object();

  void m(Properties p) {

    String s = "line.separator";

    nno = System.getProperty("line.separator");
    // :: error: (assignment)
    nno = System.getProperty(s);
    // :: error: (assignment)
    nno = System.getProperty("not.a.builtin.property");

    // :: error: (assignment)
    nno = p.getProperty("line.separator");
    // :: error: (assignment)
    nno = p.getProperty(s);
    // :: error: (assignment)
    nno = p.getProperty("not.a.builtin.property");
  }
}
