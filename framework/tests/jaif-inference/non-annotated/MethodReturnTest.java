import tests.jaifinference.qual.*;
public class MethodReturnTest {

    static int getSibling1() {
        return (@Sibling1 int) 0;
    }

    public static @Sibling1 int getSibling1NotAnnotated() {
        //:: error: (return.type.incompatible)
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

    public @Parent int getParent() {
        int x = lubTest();
        //:: error: (return.type.incompatible)
        return x;
    }
}

