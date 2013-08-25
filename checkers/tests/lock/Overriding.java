import checkers.lock.quals.*;

public class Overriding {

    class SuperClass {
        @Holding("a")
        void guardedByOne() { }

        @Holding({"a", "b"})
        void guardedByTwo() { }

        @Holding({"a", "b", "c"})
        void guardedByThree() { }

    }

    class SubClass extends SuperClass {
        @Holding({"a", "b"})  // error
          //:: error: (override.holding.invalid)
        @Override void guardedByOne() { }

        @Holding({"a", "b"})
        @Override void guardedByTwo() { }

        @Holding({"a", "b"})
        @Override void guardedByThree() { }
    }
}
