import tests.jaifinference.qual.*;
public class MethodReturnTest {

    static @Sibling1 int getSibling1() {
        return (@Sibling1 int) 0;
    }

    public static int getSibling1NotAnnotated() {
        return getSibling1();
    }

    public static boolean bool = false;

    public static int lubTest() {
        if (bool) {
            return (@Sibling1 int) 0;
        } else {
            return (@Sibling2 int) 0;
        }
    }
    class InnerClass {
        public @Sibling1 int getSibling1NotAnnotated2() {
            int x = MethodReturnTest.getSibling1NotAnnotated();
            return x;
        }

        public @JaifBottom int getBottomWrong() {
            int x = MethodReturnTest.getSibling1NotAnnotated();
            //:: error: (return.type.incompatible)
            return x;
        }

        public @Sibling1 int getSibling1Wrong() {
            int x = MethodReturnTest.lubTest();
            //:: error: (return.type.incompatible)
            return x;
        }

        public @Sibling2 int getSibling2Wrong() {
            int x = MethodReturnTest.lubTest();
            //:: error: (return.type.incompatible)
            return x;
        }

        public @Parent int getParent() {
            int x = MethodReturnTest.lubTest();
            return x;
        }
    }
}

