
import java.util.Comparator;

class List<K0> {}

@SuppressWarnings({"javari"})
abstract class Ordering<T> implements Comparator<T> {
    // Natural order

    public static <C extends Comparable> Ordering<C> natural() {
        return new Ordering<C>() {
            @Override
            public int compare(C o1, C o2) {
                return 0;
            }
        };
    }
}

@SuppressWarnings("javari")
class PolyCollectorTypeVars {
  //Both of these come from the extends Comparable on line 9
  @SuppressWarnings({"rawtypes","type.argument.type.incompatible"})
  public static List<Comparable> treeKeys2() {
      //See Limitation in DefaultTypeArgumentInference on interdependent methods
      return treeKeys(Ordering.natural());
  }

  public static <K0> List<K0> treeKeys(final Comparator<K0> comparator) {
    return new List<K0>();
  }
}
