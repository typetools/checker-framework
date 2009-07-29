import checkers.igj.quals.*;
import java.util.*;

public class Flow {
    <T> @Immutable List<T> emptyList() { return null; }

    public void testFlow() {

        @Mutable List<String> m = emptyList();   // error

        List<String> im = emptyList();
        im.add("m"); // error
    }
}
