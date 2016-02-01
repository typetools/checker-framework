import tests.signatureinference.qual.Sibling1;
import tests.signatureinference.qual.Parent;
import tests.signatureinference.qual.*;
public class MethodReturnTest {

    static @Sibling1 int getSibling1NotAnnotated() {
        return (@Sibling1 int) 0;
    }

    static @Sibling1 int getSibling1() {
        return getSibling1NotAnnotated();
    }
    public static boolean bool = false;

    public static @Parent int lubTest() {
        if (bool) {
            return (@Sibling1 int) 0;
        } else {
            return (@Sibling2 int) 0;
        }
    }

    public static @Parent int getParent() {
        int x = lubTest();
        return x;
    }

    class InnerClass {
        @Parent
        int field = 0;
        @Parent
        int getParent2() {
            field = getParent();
            return getParent();
        }

        void receivesSibling1(@Sibling1 int i) {
            expectsSibling1(i);
        }

        void expectsSibling1(@Sibling1 int i) {}
        void test() {
            @Sibling1 int sib = (@Sibling1 int) 0;
            receivesSibling1(sib);
        }
    }
}

