import tests.signatureinference.qual.SiblingWithFields;
import tests.signatureinference.qual.DefaultType;
import tests.signatureinference.qual.Sibling2;
import tests.signatureinference.qual.Sibling1;
import tests.signatureinference.qual.Parent;
import tests.signatureinference.qual.*;
public class MethodReturnTest {

    static @Sibling1 int getSibling1() {
        return (@Sibling1 int) 0;
    }

    public static @Sibling1 int getSibling1NotAnnotated() {
        return getSibling1();
    }

    public static @DefaultType boolean bool = false;

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

    public void voidMethod() {
        return;
    }
}

