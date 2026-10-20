// A regression test; see IteratorConflictingAnnosTypeTuple.java, which declares the iterator()
// method that this file calls.

import java.util.Iterator;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

public class IteratorConflictingAnnosUse {
  private final IteratorConflictingAnnosTypeTuple inputTypes =
      new IteratorConflictingAnnosTypeTuple();

  @Unmodifiable Iterator<String> reproduce() {
    return inputTypes.iterator();
  }
}
