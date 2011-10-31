import checkers.interning.quals.*;

import java.util.*;

public abstract class Subclass
    implements Comparable<Subclass>  // note non-generic
{

  public int compareTo(Subclass other) {
      return 0;
  }

}
