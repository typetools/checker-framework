import static java.util.Arrays.asList;

import java.util.List;

@SuppressWarnings("all") // Just check for crashes.
public class Issue2721 {
    void foo() {
        passThrough(asList(asList(1))).get(0).get(0).intValue();
    }

    <T> List<? extends T> passThrough(List<? extends T> object) {
        return object;
    }
}
