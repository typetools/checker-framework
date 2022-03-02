import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.ArrayList;
import java.util.List;

public class SignednessAnnotationError {
    void test() {
        List<@KeyFor("hello") String> s = new ArrayList<>();
        @KeyFor("hell") Object o = new Object();
    }
}
