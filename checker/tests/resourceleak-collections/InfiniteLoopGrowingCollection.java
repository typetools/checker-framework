import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * Tests the check for growing an owning collection where control cannot reach a method exit, as
 * inside an infinite loop.
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
   * obligation can never be enforced at a method exit.
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

  /*
   * The method never returns normally, but the obligation reaches the exceptional exit, where it is
   * checked.  The loop disposes of every element first, so there is no error.
   */
  void disposeThenThrow() throws IOException {
    List<LoopResource> resources = new ArrayList<>();
    resources.add(new LoopResource());
    for (LoopResource resource : resources) {
      resource.close();
    }
    throw new IOException();
  }
}
