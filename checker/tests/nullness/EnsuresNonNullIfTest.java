import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class MyNode {
    int id;
    @Nullable MyNode next;

    MyNode(int id, @Nullable MyNode next) {
        this.id = id;
        this.next = next;
    }
}

class MyEnumerate {
    private @Nullable MyNode current;

    public MyEnumerate(MyNode node) {
        this.current = node;
    }

    @EnsuresNonNullIf(expression = "current", result = true)
    public boolean hasMoreElements() {
        return (current != null);
    }

    @RequiresNonNull("current")
    public MyNode nextElement() {
        MyNode retval = current;
        current = current.next;
        return retval;
    }
}

class MyMain {
    public static final void main(String args[]) {
        MyNode n2 = new MyNode(2, null);
        MyNode n1 = new MyNode(1, n2);
        MyEnumerate e = new MyEnumerate(n1);
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement().id);
        }
    }
}
