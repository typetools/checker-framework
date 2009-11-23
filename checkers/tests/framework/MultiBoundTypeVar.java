import checkers.util.test.*;

public class MultiBoundTypeVar {

    <T extends @Odd Number & Cloneable & @Odd Appendable> void test(T t) {
        Number n1 = t;
        @Odd Number n2 = t;

        Cloneable c1 = t;
        @Odd Cloneable c2 = t;  // error

        Appendable d1 = t;
        @Odd Appendable d2 = t;
    }

}
