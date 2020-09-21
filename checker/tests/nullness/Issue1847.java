import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class Issue1847 {
    final Map<String, String> map = new HashMap<>();

    public void test() {
        // Should give null error here:
        // :: error: (dereference.of.nullable)
        withLookup((String myVar) -> map.get(myVar).length());
        for (Iterator<Map.Entry<@KeyFor("map") String, String>> iterator =
                        map.entrySet().iterator();
                iterator.hasNext(); ) {
            Map.Entry<@KeyFor("map") String, String> entry = iterator.next();
            // Problem is that myVar gets inferred as @KeyFor("map") here,
            // and this variable is not distinguished from the lambda variables of the same name,
            // even though their scopes do not overlap and they are different variables.
            // Change this variable name to myVar2 and you will see the null errors on the lambdas:
            String myVar = entry.getKey();
        }

        // Should also give null error here:
        // :: error: (dereference.of.nullable)
        withLookup(myVar -> map.get(myVar).length());
    }

    public void withLookup(Function<String, Integer> getFromMap) {}
}
