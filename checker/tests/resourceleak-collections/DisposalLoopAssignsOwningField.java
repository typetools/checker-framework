// A disposal loop whose body assigns to an owning field of an object that the loop body creates.
// The loop body analysis tracks only the iterated element, so it must not check the assignment:
// it does not track `h`, so it would require a @CreatesMustCallFor annotation on the enclosing
// method.  The analysis of the whole method checks the assignment.

import java.io.Closeable;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.Nullable;

class DisposalLoopAssignsOwningField {

  @InheritableMustCall("close")
  static class Connection implements Closeable {
    @Override
    public void close() {}
  }

  @InheritableMustCall("close")
  static class Holder implements Closeable {
    @Owning @Nullable Connection current;

    @Override
    @EnsuresCalledMethods(value = "this.current", methods = "close")
    public void close() {
      if (current != null) {
        current.close();
      }
    }
  }

  void assignFieldOfLocal(@OwningCollection List<Resource> resources) {
    for (Resource r : resources) {
      Holder h = new Holder();
      h.close();
      h.current = new Connection();
      h.close();
      r.close();
      r.flush();
    }
  }
}
