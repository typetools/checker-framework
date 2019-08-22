import static java.util.Arrays.asList;

import java.util.List;

@SuppressWarnings("") // Just check for crashed.
class Issue2721 {
    void foo() {
        passThrough(asList(asList(1))).get(0).get(0).intValue();
    }

    <T> List<? extends T> passThrough(List<? extends T> object) {
        return object;
    }
}
