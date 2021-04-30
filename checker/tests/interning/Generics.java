import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.checkerframework.checker.interning.qual.Interned;

public class Generics {

  void testGenerics() {

    Map<String, @Interned String> map = null;
    map = new HashMap<>();

    String a = new String("foo");
    @Interned String b = "bar";

    String notInterned;
    @Interned String interned;

    map.put(a, b); // valid
    // :: error: (argument)
    map.put(b, a); // error

    notInterned = map.get(a); // valid
    interned = map.get(b); // valid

    Collection<@Interned String> internedSet;
    Collection<String> notInternedSet;

    notInternedSet = map.keySet(); // valid
    // :: error: (assignment)
    internedSet = map.keySet(); // error

    // :: error: (assignment)
    notInternedSet = map.values(); // error
    internedSet = map.values(); // valid

    HashMap<@Interned String, Vector<@Interned Integer>> all_nums = new HashMap<>();
    Vector<@Interned Integer> v = all_nums.get("Hello");
  }

  // The cells aren't interned, but their contents are
  class CellOfImm<T extends @Interned Object> {
    T value;

    boolean equals(CellOfImm<T> other) {
      return value == other.value; // valid
    }
  }

  List<@Interned String> istrings = new ArrayList<>();
  List<String> strings = new ArrayList<>();
  @Interned String istring = "interned";
  String string = new String("uninterned");

  void testGenerics2() {
    istrings.add(istring);
    // :: error: (argument)
    istrings.add(string); // invalid
    strings.add(istring);
    strings.add(string);
    istring = istrings.get(0);
    string = istrings.get(0);
    // :: error: (assignment)
    istring = strings.get(0); // invalid
    string = strings.get(0);
  }

  void testCollections() {
    Collection<String> strings = Collections.unmodifiableCollection(new ArrayList<String>());

    Collection<@Interned String> istrings =
        Collections.unmodifiableCollection(new ArrayList<@Interned String>()); // valid
  }

  class MyList extends ArrayList<@Interned String> {
    // Correct return value is Iterator<@Interned String>
    // :: error: (override.return)
    public Iterator<String> iterator() {
      return null;
    }
  }

  // from VarInfoAux
  static class VIA {
    private static VIA theDefault = new VIA();
    private Map<@Interned String, @Interned String> map;

    void testMap() {
      Map<@Interned String, @Interned String> mymap;
      mymap = theDefault.map;
      mymap = new HashMap<@Interned String, @Interned String>(theDefault.map);
      mymap = new HashMap<>(theDefault.map);
    }
  }

  // type inference
  <T> T id(T m, Object t) {
    return m;
  }

  void useID() {
    String o = id("m", null);
  }

  // raw types again
  void testRawTypes() {
    ArrayList lst = null;
    Collections.sort(lst);
  }

  public static class Pair<T1, T2> {
    public T1 a;
    public T2 b;

    public Pair(T1 a, T2 b) {
      this.a = a;
      this.b = b;
    }

    /** Factory method with short name and no need to name type parameters. */
    public static <A, B> Pair<A, B> of(A a, B b) {
      return new Pair<>(a, b);
    }
  }

  static class C<T> {
    T next1;

    // @skip-test
    // This test might be faulty
    //        private Pair<T,T> return1() {
    //            Pair<T,T> result = Pair.of(next1, (T)null);
    //            return result;
    //        }
  }
}
