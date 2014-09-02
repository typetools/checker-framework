import org.checkerframework.checker.interning.qual.*;

import java.util.*;

import org.checkerframework.dataflow.qual.Pure;

public abstract class Subclass
    implements Comparable<Subclass>  // note non-generic
{

  @Pure
  public int compareTo(Subclass other) {
      return 0;
  }

}
