// Test case for https://github.com/typetools/checker-framework/issues/5971

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;

@InheritableMustCall("disconnect")
public class OwnershipTransferAtReassignment {

  private @Owning @Nullable Node head = null;

  @CreatesMustCallFor("this")
  public boolean add() {
    head = new Node(head);
    return true;
  }

  @EnsuresCalledMethods(value = "this.head", methods = "disconnect")
  public void disconnect() {
    head.disconnect();
  }

  @InheritableMustCall("disconnect")
  private static class Node {
    @Owning private final @Nullable Node next;

    public Node(@Owning @Nullable Node next) {
      this.next = next;
    }

    @EnsuresCalledMethods(value = "this.next", methods = "disconnect")
    public void disconnect() {
      next.disconnect();
    }
  }
}
