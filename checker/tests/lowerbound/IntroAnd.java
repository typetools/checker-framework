import org.checkerframework.checker.lowerbound.qual.*;

class IntroAnd {
    void test() {
        @NonNegative int a = 1 & 0;
        @NonNegative int b = a & 5;

        //:: error: (assignment.type.incompatible)
        @Positive int c = a & b;
        @NonNegative int d = a & b;
    }
}
