import tests.util.*;
import java.util.*;

public @Odd class ClassAnnotations {

    ClassAnnotations c;

    public void test() {
        @Odd ClassAnnotations d = c;
    }
}
