import org.checkerframework.checker.lowerbound.qual.*;

public class Boilerplate {

    void test() {
        //:: error: (assignment.type.incompatible)
        @Positive int a = -1;
    }
}
//a comment
