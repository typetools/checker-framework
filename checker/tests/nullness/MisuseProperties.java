import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;

public class MisuseProperties {

  void propertiesToHashtable(Properties p) {
    // :: error: (argument)
    p.setProperty("line.separator", null);
    // :: error: (argument)
    p.put("line.separator", null);
    Hashtable h = p;
    // Error, because HashTable value has NonNull bound.
    // TODO: false negative. See #365.
    //// :: error: (argument) :: warning: [unchecked] unchecked call to
    //// put(K,V) as a member of the raw type java.util.Hashtable
    // :: warning: [unchecked] unchecked call to put(K,V) as a member of the raw type
    // java.util.Hashtable
    h.put("line.separator", null);
    // :: error: (argument)
    System.setProperty("line.separator", null);

    Dictionary d1 = p;
    // No error, because Dictionary value has Nullable bound.
    // :: warning: [unchecked] unchecked call to put(K,V) as a member of the raw type
    // java.util.Dictionary
    d1.put("line.separator", null);

    // :: error: (assignment)
    Dictionary<Object, @Nullable Object> d2 = p;
    d2.put("line.separator", null);

    // :: error: (clear.system.property)
    System.setProperties(p); // OK; p has no null values

    System.clearProperty("foo.bar"); // OK

    // Each of the following should cause an error, because it leaves line.separator null.

    // These first few need to be special-cased, I think:

    // :: error: (clear.system.property)
    System.clearProperty("line.separator");

    p.remove("line.separator");
    p.clear();

    // These are OK because they seem to only add, not remove, properties:
    // p.load(InputStream), p.load(Reader), p.loadFromXML(InputStream)

    // The following problems are a result of treating a Properties as one
    // of its supertypes.  Here are some solutions:
    //  * Forbid treating a Properties object as any of its supertypes.
    //  * Create an annotation on a Properties object, such as
    //    @HasSystemProperties, and forbid some operations (or any
    //    treatment as a supertype) for such properties.

    Set<@KeyFor("p") Object> keys = p.keySet();
    // now remove  "line.separator" from the set
    keys.remove("line.separator");
    keys.removeAll(keys);
    keys.clear();
    keys.retainAll(Collections.EMPTY_SET);

    Set<Map.Entry<@KeyFor("p") Object, Object>> entries = p.entrySet();
    // now remove the pair containing "line.separator" from the set, as above

    Collection<Object> values = p.values();
    // now remove the line separator value from values, as above

    Hashtable h9 = p;
    h9.remove("line.separator");
    h9.clear();
    // also access via entrySet, keySet, values

    Dictionary d9 = p;
    d9.remove("line.separator");
  }
}
