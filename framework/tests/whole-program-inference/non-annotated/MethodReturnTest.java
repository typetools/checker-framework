import tests.wholeprograminference.qual.*;
public class MethodReturnTest {

    static int getSibling1NotAnnotated() {
        return (@Sibling1 int) 0;
    }

    static @Sibling1 int getSibling1() {
        //:: error: (return.type.incompatible)
        return getSibling1NotAnnotated();
    }
    public static boolean bool = false;

    public static int lubTest() {
        if (bool) {
            return (@Sibling1 int) 0;
        } else {
            return (@Sibling2 int) 0;
        }
    }

    public static @Parent int getParent() {
        int x = lubTest();
        //:: error: (return.type.incompatible)
        return x;
    }

    class InnerClass {
        int field = 0;
        int getParent2() {
            field = getParent();
            return getParent();
        }

        void receivesSibling1(int i) {
            //:: error: (argument.type.incompatible)
            expectsSibling1(i);
        }

        void expectsSibling1(@Sibling1 int i) {}
        void test() {
            @Sibling1 int sib = (@Sibling1 int) 0;
            receivesSibling1(sib);
        }
    }
}
