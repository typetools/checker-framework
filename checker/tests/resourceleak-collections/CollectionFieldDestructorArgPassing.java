import java.io.IOException;
import java.util.*;
import java.util.Iterator;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests collection field destructors that pass the owning collection to helpers or discharge
 * it through a while loop.
 */
class CollectionFieldDestructorArgPassing {

  @InheritableMustCall("close")
  static class ThrowingCloseResource {
    void close() throws IOException {}
  }

  @InheritableMustCall("shutdownConnections")
  static class Worker {

    final @OwningCollection List<ThrowingCloseResource> tcpConnections = new ArrayList<>();

    @CreatesMustCallFor("this")
    void add(@Owning ThrowingCloseResource resource) {
      tcpConnections.add(resource);
    }

    @CollectionFieldDestructor("this.tcpConnections")
    /*
     * Delegating the field to another method should not satisfy the field destructor
     * contract, because it attempts to transfer ownership away from the field.
     */
    // ::error: contracts.postcondition
    void shutdownConnections() throws IOException {
      // ::error: transfer.owningcollection.field.ownership
      fieldCloser(this.tcpConnections);
    }

    void fieldCloser(@OwningCollection List<ThrowingCloseResource> tcpConnections) {
      for (ThrowingCloseResource r : tcpConnections) {
        try {
          r.close();
        } catch (IOException e) {
        }
      }
    }
  }

  @InheritableMustCall("shutdownConnections")
  static class Worker2 {

    final @OwningCollection List<ThrowingCloseResource> tcpConnections = new ArrayList<>();

    @CreatesMustCallFor("this")
    void add(@Owning ThrowingCloseResource resource) {
      tcpConnections.add(resource);
    }

    @CollectionFieldDestructor("this.tcpConnections")
    /*
     * A while loop that iterates the field directly should satisfy the field destructor
     * contract.
     */
    void shutdownConnections() throws IOException {
      Iterator<ThrowingCloseResource> it = tcpConnections.iterator();
      while (it.hasNext()) {
        ThrowingCloseResource r = it.next();
        try {
          r.close();
        } catch (IOException e) {
        }
      }
    }
  }

  static void checkArgIsOCWO(
      // :: error: illegal.type.annotation
      @OwningCollectionWithoutObligation Iterable<ThrowingCloseResource> arg) {}

  /*
   * Check that the helper-based destructor still leaves the wrapper object with an
   * outstanding must-call obligation.
   */
  void client() throws Exception {
    Worker w = new Worker();
    w.add(new ThrowingCloseResource());
    w.shutdownConnections();
  }

  /*
   * Check the same helper-based case when the wrapper is used under ordinary exception
   * handling.
   */
  void client2() {
    Worker w = new Worker();
    w.add(new ThrowingCloseResource());
    try {
      w.shutdownConnections();
    } catch (IOException e) {
    }
  }

  /*
   * Sanity-check the resource type itself outside of any collection obligations.
   */
  void client3() {
    ThrowingCloseResource r = new ThrowingCloseResource();
    try {
      r.close();
    } catch (IOException e) {
    }
  }
}
