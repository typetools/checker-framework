import checkers.nullness.quals.Nullable;

class GenericTest12b {
    class Cell<T1 extends @Nullable Object> {}

    class Node<CONTENT extends @Nullable Object> {
        public Node(Cell<CONTENT> userObject) { }
        void nodecall(Cell<CONTENT> userObject) {}
    }

    class RootNode extends Node<Void> {
        public RootNode() {
            super(new Cell<Void>());
            call(new Cell<Void>());
            nodecall(new Cell<Void>());
        }
        void call(Cell<Void> userObject) {}
    }
}