import java.io.Serializable;
import java.util.Comparator;

//@skip-test
// error: Expected the type of TYPE_CAST tree in assignment context
//        to be a functional interface.
class Issue478 {
  public static Comparator<Object> allTheSame() {
    // cast fails with any checker but succeeds with no processors
    return (Comparator<Object> & Serializable) (c1, c2) -> 0;
  }
}

