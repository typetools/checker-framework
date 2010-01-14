import checkers.util.test.*;

public class AnonymousClasses {

    void test() {
        new Object() {
            @Odd Object o = this; // error
        };

        new @Odd Object() {
            @Odd Object o = this;
        };
    }
}
