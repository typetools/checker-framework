import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;

// This example is taken from the FBC paper, figure 1 (and has some additional code in main below).
// We made the list generic.
public class List<T> {
    @NotOnlyInitialized
    Node<T> sentinel;

    public List() {
        this.sentinel = new Node<T>(this);
    }

    void insert(@Nullable T data) {
        this.sentinel.insertAfter(data);
    }

    public static void main() {
        List<Integer> l = new List<Integer>();
        l.insert(1);
        l.insert(2);
    }
}

class Node<T> {
    @NotOnlyInitialized
    Node<T> prev;

    @NotOnlyInitialized
    Node<T> next;

    @NotOnlyInitialized
    List parent;

    @Nullable
    T data;

    // for sentinel construction
    Node(@UnderInitialization List parent) {
        this.parent = parent;
        this.prev = this;
        this.next = this;
    }

    // for data node construction
    Node(Node<T> prev, Node<T> next, @Nullable T data) {
        this.parent = prev.parent;
        this.prev = prev;
        this.next = next;
        this.data = data;
    }

    void insertAfter(@Nullable T data) {
        Node<T> n = new Node<T>(this, this.next, data);
        this.next.prev = n;
        this.next = n;
    }
}
