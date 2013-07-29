import java.util.Map;
import java.util.AbstractMap;

import checkers.quals.*;
import checkers.nullness.quals.*;
import checkers.regex.quals.*;
import checkers.initialization.quals.*;

//:: error: (initialization.fields.uninitialized)
public abstract class WeakHasherMap<K, V> extends AbstractMap<K, V> implements
        Map<K, V> {
    private Map<Object, V> hash;

    @dataflow.quals.Pure
    //:: error: (override.param.invalid)
    public boolean containsKey(Object key) {
        //:: warning: [unchecked] unchecked cast
        K kkey = (K) key;
        hash.containsKey(null);
        return true;
    }
}
