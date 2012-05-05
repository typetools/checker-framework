import checkers.util.test.*;
import java.util.*;

public @Odd class ClassAnnotations {

    ClassAnnotations c;

    public void test() {
        @Odd ClassAnnotations d = c;
    }
}
