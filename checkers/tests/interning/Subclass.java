import checkers.interning.quals.*;

import java.util.*;

import dataflow.quals.Pure;

public abstract class Subclass
    implements Comparable<Subclass>  // note non-generic
{

  @Pure
  public int compareTo(Subclass other) {
      return 0;
  }

}
