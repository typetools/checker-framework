import tests.jaifinference.qual.SiblingWithFields;
import tests.jaifinference.qual.DefaultType;
import tests.jaifinference.qual.Parent;
import tests.jaifinference.qual.Sibling1;
import tests.jaifinference.qual.*;
public class MethodReturnTest {

    static @Sibling1 int getSibling1() {
        return (@Sibling1 int) 0;
    }

    public static @Sibling1 int getSibling1NotAnnotated() {
        return getSibling1();
    }

    @DefaultType
    public static boolean bool = false;

    public static @Parent int lubTest() {
        if (bool) {
            return (@Sibling1 int) 0;
        } else {
            return (@Sibling2 int) 0;
        }
    }

    public @Parent int getParent() {
        int x = lubTest();
        return x;
    }
}

