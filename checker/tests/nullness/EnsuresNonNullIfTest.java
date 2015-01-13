import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class Node {
    int id;
    @Nullable Node next;

    Node(int id, @Nullable Node next) {
        this.id = id;
        this.next = next;
    }
}

class Enumerate {
    private @Nullable Node current;

    public Enumerate(Node node) {
        this.current = node;
    }

    @EnsuresNonNullIf(expression="current", result=true)
    public boolean hasMoreElements() {
        return (current != null);
    }

    @RequiresNonNull("current")
    public Node nextElement() {
        Node retval = current;
        current = current.next;
        return retval;
    }
}

class Main {
    public static final void main(String args[]) {
        Node n2 = new Node(2, null);
        Node n1 = new Node(1, n2);
        Enumerate e = new Enumerate(n1);
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement().id);
        }
    }
}