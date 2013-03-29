import java.util.*;

class MethodTypeVars3 {
    public static
    <T> Map<T,List<T>> dominators(Map<T,List<T>> preds) {
        List<T> nodes = new ArrayList<T>(preds.keySet());

        // Compute roots & non-roots, for convenience
        List<T> roots = new ArrayList<T>();
        List<T> non_roots = new ArrayList<T>();

        Map<T,List<T>> dom = new HashMap<T,List<T>>();

        // Initialize result:  for roots just the root, otherwise everything
        for (T node : preds.keySet()) {
            if (preds.get(node).isEmpty()) {
                // This is a root
                roots.add(node);
                // Its only dominator is itself.
                Set<T> set = Collections.singleton(node);
                dom.put(node, new ArrayList<T>(set));

                dom.put(node, new ArrayList<T>(Collections.singleton(node)));
            } else {
                non_roots.add(node);
                dom.put(node, new ArrayList<T>(nodes));
            }
        }
        
        return dom;
    }
}