import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1847B {
    public void test1() {
        // :: error: (dereference.of.nullable)
        Function<@Nullable String, String> f1 = (@Nullable String myVar) -> myVar.toString();
        {
            String myVar = "hello";
        }
        // :: error: (dereference.of.nullable)
        Function<@Nullable String, String> f2 = (@Nullable String myVar) -> myVar.toString();
    }

    public void test2() {
        // :: error: (dereference.of.nullable)
        Function<String, String> f1 = (@Nullable String myVar) -> myVar.toString();
    }
}
