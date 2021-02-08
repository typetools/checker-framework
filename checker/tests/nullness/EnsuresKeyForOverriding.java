import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;

public class EnsuresKeyForOverriding {
    static class MyClass {
        Object field = new Object();
    }

    MyClass o = new MyClass();

    @EnsuresKeyFor(value = "#1.field", map = "#2")
    void method(MyClass o, Map<Object, Object> map) {
        map.put(o.field, "Hello");
    }

    static class SubEnsuresKeyForOverriding extends EnsuresKeyForOverriding {
        @Override
        @EnsuresKeyFor(value = "#1.field", map = "#2")
        void method(MyClass q, Map<Object, Object> subMap) {
            super.method(q, subMap);
        }
    }
}
