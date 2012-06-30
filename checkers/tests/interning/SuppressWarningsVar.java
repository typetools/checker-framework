import checkers.interning.quals.*;

import java.util.*;

public class SuppressWarningsVar {

    public static void myMethod() {

        @SuppressWarnings("interning")
        @Interned String s = new String();

    }

}
