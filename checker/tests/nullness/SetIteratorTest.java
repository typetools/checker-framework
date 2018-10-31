import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class SetIteratorTest {

    private SortedSet<String> nodes;
    private Map<String, TreeMap<String, TreeSet<String>>> edges;

    public SetIteratorTest() {
        nodes = new TreeSet<String>();
        edges = new HashMap<String, TreeMap<String, TreeSet<String>>>();
    }

    public Set<String> listNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public String listChildren(String parentNode) {
        String childrenString = "";

        if (edges.get(parentNode) != null) {
            for (String childNode : edges.get(parentNode).keySet()) {
                edges.get(parentNode).toString();
                for (String childNodeEdgeX : edges.get(parentNode).get(childNode)) {
                    childrenString += " " + childNode + "(" + childNodeEdgeX + ")";
                }
            }
        }

        return childrenString;
    }

    public boolean containsNode(String node) {
        return nodes.contains(node);
    }
}
