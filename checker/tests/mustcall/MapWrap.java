// A test for a class that wraps a map. I found a similar example in Zookeeper that causes false
// positives.

import org.checkerframework.checker.mustcall.qual.*;

import java.util.HashMap;

class MapWrap<E> {
    HashMap<E, String> impl = new HashMap<E, String>();

    String remove(E e) {
        // remove should permit any object: its signature is remove(Object key), *not* remove(E key)
        String old = impl.remove(e);
        return old;
    }

    String remove2(@MustCall({}) E e) {
        // remove should permit any object: its signature is remove(Object key), *not* remove(E key)
        String old = impl.remove(e);
        return old;
    }
}
