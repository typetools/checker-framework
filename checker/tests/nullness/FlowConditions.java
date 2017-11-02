import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;

class FlowConditions {
    void m(@Nullable Object x, @Nullable Object y) {
        if (x == null || y == null) {
            // :: error: (dereference.of.nullable)
            x.toString();
            // :: error: (dereference.of.nullable)
            y.toString();
        } else {
            x.toString();
            y.toString();
        }
    }

    private final Map<String, Set<String>> graph = new HashMap<String, Set<String>>();

    public void addEdge1(String e, String parent, String child) {
        if (!graph.containsKey(parent)) {
            throw new NoSuchElementException();
        }
        if (!graph.containsKey(child)) {
            throw new NoSuchElementException();
        }
        @NonNull Set<String> edges = graph.get(parent);
    }

    // TODO: Re-enable when issue 221 is resolved.
    // public void addEdge2(String e, String parent, String child) {
    //     if ( (!graph.containsKey(parent)) ||
    //          (!graph.containsKey(child)))
    //         throw new NoSuchElementException();
    //     @NonNull Set<String> edges = graph.get(parent);
    // }

}
