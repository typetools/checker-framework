import checkers.interning.quals.*;

import java.util.*;

public class FlowInterning {

  public boolean isSame(Object a, Object b) {
    return ((a == null)
            ? (a == b)
            : (a.equals(b)));
  }

}
