import org.checkerframework.checker.nullness.qual.*;

class Node {
  int id;
  @Nullable Node next;

  Node(int id, @Nullable Node next) {
    this.id = id;
    this.next = next;
  }
}

class SubEnumerate {
  protected @Nullable Node current;

  public SubEnumerate(Node node) {
    this.current = node;
  }

  @EnsuresNonNullIf(expression = "current", result = true)
  public boolean hasMoreElements() {
    return (current != null);
  }
}

class Enumerate extends SubEnumerate {

  public Enumerate(Node node) {
    super(node);
  }

  public boolean hasMoreElements() {
    return (current != null);
  }
}

class Main {
  public static final void main(String args[]) {
    Node n2 = new Node(2, null);
    Node n1 = new Node(1, n2);
    Enumerate e = new Enumerate(n1);
    while (e.hasMoreElements()) {}
  }
}
