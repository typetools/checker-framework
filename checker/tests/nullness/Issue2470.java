import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Issue2470 {
    static class Example {
        @MonotonicNonNull String s;

        public Example() {}

        @EnsuresNonNull("this.s")
        public Example setS(String s1) {
            this.s = s1;
            return this;
        }

        @RequiresNonNull("this.s")
        public void print() {
            System.out.println(this.s.toString());
        }
    }

    static void buggy() {
        new Example()
                // :: error: (contracts.precondition.not.satisfied)
                .print();
    }

    static void ok() {
        // :: error:(contracts.precondition.not.satisfied)
        new Example().setS("test").print();
    }
}
