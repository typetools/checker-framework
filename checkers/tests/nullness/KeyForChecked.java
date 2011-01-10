import checkers.nullness.quals.*;

import java.util.*;

public class KeyForChecked {

// Taken from the annotated JDK, because tests execute without the JDK.
interface KFMap<K extends @NonNull Object, V extends @NonNull Object> {
	public static interface Entry<@Covariant K extends @Nullable Object, V extends @Nullable Object> {
		K getKey();
	    V getValue();
	}
	@Pure boolean containsKey(@Nullable Object a1);
	@Pure @Nullable V get(@Nullable Object a1);
	@Nullable V put(K a1, V a2);
	Set<@KeyFor("this") K> keySet();
	Set<KFMap.Entry<@KeyFor("this") K, V>> entrySet();
	KFIterator<K> iterator();
}

class KFHashMap<K extends @NonNull Object, V extends @NonNull Object> implements KFMap<K, V> {
	public @Pure boolean containsKey(@Nullable Object a1) { return false; }
	public @Pure @Nullable V get(@Nullable Object a1) { return null; }
	public @Nullable V put(K a1, V a2) { return null; }
	public Set<@KeyFor("this") K> keySet() { return new HashSet<@KeyFor("this") K>(); }
	public Set<KFMap.Entry<@KeyFor("this") K, V>> entrySet() { return new HashSet<KFMap.Entry<@KeyFor("this") K, V>>(); }
	public KFIterator<K> iterator() { return new KFIterator<K>(); }
}

class KFIterator<@Covariant E extends @Nullable Object> {
	
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

    void iter() {
    	KFMap<String, Object> emap = new KFHashMap<String, Object>();
    	Set<@KeyFor("emap") String> s = emap.keySet();
    	Iterator<@KeyFor("emap") String> it = emap.keySet().iterator();
    	Iterator<@KeyFor("emap") String> it2 = s.iterator();
    	
    	Collection<@KeyFor("emap") String> x = Collections.unmodifiableSet(emap.keySet());
    	
    	for (@KeyFor("emap") String st : s) {}
    	for (String st : s) {}
    	
    	//:: (enhancedfor.type.incompatible)
    	for (@KeyFor("bubu") String st : s) {}
    }
    
    <T> void dominators(KFMap<T,List<T>> preds) {
    	for (T node : preds.keySet()) {}
    	
    	for (@KeyFor("preds") T node : preds.keySet()) {}
    }
    
    void entrySet() {
    	KFMap<String, Object> emap = new KFHashMap<String, Object>();
    	Set<KFMap.Entry<@KeyFor("emap") String, Object>> es = emap.entrySet();
    	Set<KFMap.Entry<String, Object>> es2 = emap.entrySet();
    }
    
    public static <K,V> void mapToString(KFMap<K,V> m) {
    	Set<KFMap.Entry<K, V>> eset = m.entrySet();
    	
    	for (KFMap.Entry<K, V> entry : m.entrySet()) {}
    }
    
    void testWF(KFMap<String, Object> m) {
    	KFIterator<String> it = m.iterator();
    }
}