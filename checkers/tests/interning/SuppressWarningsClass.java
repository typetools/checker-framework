import checkers.interning.quals.*;

import java.util.*;

@SuppressWarnings("interning")
public class SuppressWarningsClass {

    public static void myMethod() {

        @Interned String s = new String();

    }

}
