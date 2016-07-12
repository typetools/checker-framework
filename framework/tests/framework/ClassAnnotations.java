import java.util.*;
import tests.util.*;

public @Odd class ClassAnnotations {

    ClassAnnotations c;

    public void test() {
        @Odd ClassAnnotations d = c;
    }
}
