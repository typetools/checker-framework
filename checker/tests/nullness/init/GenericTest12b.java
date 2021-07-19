import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GenericTest12b {
    class Cell<T1 extends @Nullable Object> {}

    class Node<CONTENT extends @Nullable Object> {
        public Node(Cell<CONTENT> userObject) {}

        void nodecall(@UnderInitialization Node<CONTENT> this, Cell<CONTENT> userObject) {}
    }

    class RootNode extends Node<Void> {
        public RootNode() {
            super(new Cell<Void>());
            call(new Cell<Void>());
            nodecall(new Cell<Void>());
        }

        void call(@UnderInitialization RootNode this, Cell<Void> userObject) {}
    }
}
