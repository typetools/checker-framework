import checkers.nullness.quals.*;
import java.util.Arrays;

public class ArrayRefs {

    public void test() {

        String[] s = null;

        if (s.length > 0)
            System.out.println("s.length > 0");

    }

    public static void test2() {
        Object a = new Object();
        Arrays.<Object>asList (new Object[] {a});
    }

}
