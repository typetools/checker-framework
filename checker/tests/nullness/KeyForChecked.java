import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.Covariant;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.IMPLICIT_UPPER_BOUND)
public class KeyForChecked {

    interface KFMap<@KeyForBottom K extends @NonNull Object, V extends @NonNull Object> {
        @Covariant(0)
        public static interface Entry<
                @KeyForBottom K1 extends @Nullable Object, V1 extends @Nullable Object> {
            K1 getKey();

            V1 getValue();
        }

        @Pure
        boolean containsKey(@Nullable Object a1);

        @Pure
        @Nullable V get(@Nullable Object a1);

        @Nullable V put(K a1, V a2);

        Set<@KeyFor("this") K> keySet();

        Set<KFMap.Entry<@KeyFor("this") K, V>> entrySet();

        KFIterator<K> iterator();
    }

    class KFHashMap<@KeyForBottom K2 extends @NonNull Object, V2 extends @NonNull Object>
            implements KFMap<K2, V2> {
        @Pure
        public boolean containsKey(@Nullable Object a1) {
            return false;
        }

        @Pure
        public @Nullable V2 get(@Nullable Object a1) {
            return null;
        }

        public @Nullable V2 put(K2 a1, V2 a2) {
            return null;
        }

        public Set<@KeyFor("this") K2> keySet() {
            return new HashSet<@KeyFor("this") K2>();
        }

        public Set<KFMap.Entry<@KeyFor("this") K2, V2>> entrySet() {
            return new HashSet<KFMap.Entry<@KeyFor("this") K2, V2>>();
        }

        public KFIterator<K2> iterator() {
            return new KFIterator<K2>();
        }
    }

    @Covariant(0)
    class KFIterator<@KeyForBottom E extends @Nullable Object> {}

    void incorrect1(Object map) {
        String nonkey = "";
        // :: error: (assignment.type.incompatible)
        @KeyFor("map") String key = nonkey;
    }

    void correct1(Object map) {
        String nonkey = "";
        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("map") String key = nonkey;
    }

    void incorrect2() {
        KFMap<String, Object> m = new KFHashMap<>();
        m.put("a", new Object());
        m.put("b", new Object());
        m.put("c", new Object());

        Collection<@KeyFor("m") String> coll = m.keySet();

        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("m") String newkey = "new";

        coll.add(newkey);
        // TODO: at this point, the @KeyFor annotation is violated
        m.put("new", new Object());
    }

    void correct2() {
        KFMap<String, Object> m = new KFHashMap<>();
        m.put("a", new Object());
        m.put("b", new Object());
        m.put("c", new Object());

        Collection<@KeyFor("m") String> coll = m.keySet();

        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("m") String newkey = "new";

        m.put(newkey, new Object());
        coll.add(newkey);
    }

    void iter() {
        KFMap<String, Object> emap = new KFHashMap<>();
        Set<@KeyFor("emap") String> s = emap.keySet();
        Iterator<@KeyFor("emap") String> it = emap.keySet().iterator();
        Iterator<@KeyFor("emap") String> it2 = s.iterator();

        Collection<@KeyFor("emap") String> x = Collections.unmodifiableSet(emap.keySet());

        for (@KeyFor("emap") String st : s) {}
        for (String st : s) {}
        Object bubu = new Object();
        // :: error: (enhancedfor.type.incompatible)
        for (@KeyFor("bubu") String st : s) {}
    }

    <T> void dominators(KFMap<T, List<T>> preds) {
        for (T node : preds.keySet()) {}

        for (@KeyFor("preds") T node : preds.keySet()) {}
    }

    void entrySet() {
        KFMap<String, Object> emap = new KFHashMap<>();
        Set<KFMap.Entry<@KeyFor("emap") String, Object>> es = emap.entrySet();

        // KeyFor has to be explicit on the component to Entry sets because
        //   a) it's not clear which map the Entry set may have come from
        //   b) and there is no guarantee the map is still accessible
        // :: error: (assignment.type.incompatible)
        Set<KFMap.Entry<String, Object>> es2 = emap.entrySet();
    }

    public static <K, V> void mapToString(KFMap<K, V> m) {
        Set<KFMap.Entry<@KeyFor("m") K, V>> eset = m.entrySet();

        for (KFMap.Entry<@KeyFor("m") K, V> entry : m.entrySet()) {}
    }

    void testWF(KFMap<String, Object> m) {
        KFIterator<String> it = m.iterator();
    }
}
