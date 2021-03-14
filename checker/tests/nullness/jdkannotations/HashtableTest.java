import java.util.Hashtable;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HashtableTest {

  public static void main(String[] args) {

    // :: error: (type.argument.type.incompatible)
    Hashtable<@Nullable Integer, String> ht1 = new Hashtable<>();

    // Suffers null pointer exception
    ht1.put(null, "hello");

    // :: error: (type.argument.type.incompatible)
    Hashtable<Integer, @Nullable String> ht2 = new Hashtable<>();

    // Suffers null pointer exception
    ht2.put(42, null);
  }
}
