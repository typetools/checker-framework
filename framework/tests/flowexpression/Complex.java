package flowexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class Complex {
    class DocCategory {
        public Map<String, String> fields = new HashMap<>();
    }

    protected DocCategory[] categories = new DocCategory[2];

    void test() {
        for (int c = 0; c < categories.length; c++) {

            for (
            @FlowExp("categories[c].fields") String field : sortedKeySet(categories[c].fields)) {
                @FlowExp("categories[c].fields") String f = field;
            }
        }
    }

    public static <K extends Comparable<? super K>, V> Collection<@FlowExp("#1") K> sortedKeySet(
            Map<K, V> m) {
        throw new RuntimeException();
    }

    private static Map<Integer, List<@FlowExp("succs1") Integer>> succs1 = new HashMap<>();

    void method() {
        Map<Integer, List<Integer>> dom1post = dominators(succs1);
    }

    public static <T> Map<T, List<T>> dominators(Map<T, List<@FlowExp("#1") T>> predecessors) {
        throw new RuntimeException();
    }
}
