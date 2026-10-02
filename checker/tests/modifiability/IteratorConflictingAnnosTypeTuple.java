// A regression test: an iterator() override whose result carries an alias annotation
// (@Unmodifiable) that expands differently in each hierarchy must not crash the checker, and must
// not conflict with the qualifiers that refineIteratorReturnType would otherwise infer.  The
// companion file IteratorConflictingAnnosUse.java calls this method.

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

class IteratorConflictingAnnosTypeTuple implements Iterable<String> {
  private final ArrayList<String> list = new ArrayList<>();

  @Override
  public @Unmodifiable Iterator<String> iterator(IteratorConflictingAnnosTypeTuple this) {
    return Collections.unmodifiableList(list).iterator();
  }
}
