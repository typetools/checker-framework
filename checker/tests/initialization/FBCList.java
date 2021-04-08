import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

// This example is taken from the FBC paper, figure 1 (and has some additional code in main below).
// We made the list generic.
public class FBCList<T> {
  @NotOnlyInitialized FBCNode<T> sentinel;

  public FBCList() {
    this.sentinel = new FBCNode<>(this);
  }

  void insert(@Nullable T data) {
    this.sentinel.insertAfter(data);
  }

  public static void main() {
    FBCList<Integer> l = new FBCList<>();
    l.insert(1);
    l.insert(2);
  }
}

class FBCNode<T> {
  @NotOnlyInitialized FBCNode<T> prev;

  @NotOnlyInitialized FBCNode<T> next;

  @NotOnlyInitialized FBCList parent;

  @Nullable T data;

  // for sentinel construction
  FBCNode(@UnderInitialization FBCList parent) {
    this.parent = parent;
    this.prev = this;
    this.next = this;
  }

  // for data node construction
  FBCNode(FBCNode<T> prev, FBCNode<T> next, @Nullable T data) {
    this.parent = prev.parent;
    this.prev = prev;
    this.next = next;
    this.data = data;
  }

  void insertAfter(@Nullable T data) {
    FBCNode<T> n = new FBCNode<>(this, this.next, data);
    this.next.prev = n;
    this.next = n;
  }
}
