import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;

class MethodTypeVars3 {
    public static <@KeyFor("#1") T extends @KeyFor("#1") Object> Map<T, List<T>> dominators(
            Map<T, List<T>> preds) {
        List<T> nodes = new ArrayList<>(preds.keySet());

        // Compute roots & non-roots, for convenience
        List<@KeyFor("preds") T> roots = new ArrayList<>();
        List<@KeyFor("preds") T> non_roots = new ArrayList<>();

        Map<@KeyFor("preds") T, List<T>> dom = new HashMap<>();

        // Initialize result:  for roots just the root, otherwise everything
        for (@KeyFor("preds") T node : preds.keySet()) {
            if (preds.get(node).isEmpty()) {
                // This is a root
                roots.add(node);
                // Its only dominator is itself.
                Set<@KeyFor("preds") T> set = Collections.singleton(node);

                dom.put(node, new ArrayList<T>(set));

                dom.put(node, new ArrayList<T>(Collections.singleton(node)));
            } else {
                non_roots.add(node);
                dom.put(node, new ArrayList<T>(nodes));
            }
        }

        return dom;
    }

    <XXX extends Object> void test(Map<XXX, List<XXX>> dom, XXX node) {
        dom.put(node, new ArrayList<XXX>(Collections.singleton(node)));
    }
}
