import checkers.nullness.quals.*;

import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

public class KeyForChecked {

// Taken from the annotated JDK, because tests execute without the JDK.
interface KFMap<K extends @NonNull Object, V extends @NonNull Object> {
	public static interface Entry<K extends @Nullable Object, V extends @Nullable Object> {
		K getKey();
	    V getValue();
	}
	@Pure boolean containsKey(@Nullable Object a1);
	@Pure @Nullable V get(@Nullable Object a1);
	@Nullable V put(K a1, V a2);
	Set<@KeyFor("this") K> keySet();
	Set<KFMap.Entry<@KeyFor("this") K, V>> entrySet();
}

class KFHashMap<K extends @NonNull Object, V extends @NonNull Object> implements KFMap<K, V> {
	public @Pure boolean containsKey(@Nullable Object a1) { return false; }
	public @Pure @Nullable V get(@Nullable Object a1) { return null; }
	public @Nullable V put(K a1, V a2) { return null; }
	public Set<@KeyFor("this") K> keySet() { return new HashSet<@KeyFor("this") K>(); }
	public Set<KFMap.Entry<@KeyFor("this") K, V>> entrySet() { return new HashSet<KFMap.Entry<@KeyFor("this") K, V>>(); }
}

	
    void incorrect1() {
        String nonkey = "";
        //:: (assignment.type.incompatible)
        @KeyFor("map") String key = nonkey;
    }
    
    void correct1() {
        String nonkey = "";
        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("map") String key = nonkey;
    }
    
    void incorrect2() {
    	KFMap<String, Object> m = new KFHashMap<String, Object>();
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
    	KFMap<String, Object> m = new KFHashMap<String, Object>();
    	m.put("a", new Object());
    	m.put("b", new Object());
    	m.put("c", new Object());
    	
    	Collection<@KeyFor("m") String> coll = m.keySet();
    	
    	@SuppressWarnings("assignment.type.incompatible")
    	@KeyFor("m") String newkey = "new";

    	m.put(newkey, new Object());
    	coll.add(newkey);
    }
}