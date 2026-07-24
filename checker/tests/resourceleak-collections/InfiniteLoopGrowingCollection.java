import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * Tests the normal-exit check for growing an owning collection inside a loop that never
 * reaches a regular exit.
 */
class InfiniteLoopGrowingCollection {

  @InheritableMustCall("close")
  static class LoopResource implements Closeable {
    @Override
    public void close() {}
  }

  @Nullable LoopResource maybeAccept() {
    return null;
  }

  /*
   * Adding a resource to the collection inside an infinite loop should report that the
   * obligation can never be enforced at a regular exit.
   */
  void serverLoop() {
    @OwningCollection List<LoopResource> resources = new ArrayList<>();
    while (true) {
      LoopResource resource = maybeAccept();
      if (resource != null) {
        // :: error: collection.obligation.never.enforced
        resources.add(resource);
      }
    }
  }
}
