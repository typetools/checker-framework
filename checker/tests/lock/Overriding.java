import org.checkerframework.checker.lock.qual.*;

public class Overriding {

    class SuperClass {
        protected Object a, b, c;

        @HoldingOnEntry("a")
        void guardedByOne() { }

        @HoldingOnEntry({"a", "b"})
        void guardedByTwo() { }

        @HoldingOnEntry({"a", "b", "c"})
        void guardedByThree() { }

    }

    class SubClass extends SuperClass {
        @HoldingOnEntry({"a", "b"})  // error
          //:: error: (override.holding.invalid)
        @Override void guardedByOne() { }

        @HoldingOnEntry({"a", "b"})
        @Override void guardedByTwo() { }

        @HoldingOnEntry({"a", "b"})
        @Override void guardedByThree() { }
    }
}
