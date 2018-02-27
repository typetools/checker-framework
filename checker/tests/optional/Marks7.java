import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Test cases for Rule #7: "Don't use an Optional to wrap any collection type (List, Set, Map).
 * Instead, use an empty collection to represent the absence of values.
 */
public class Marks7 {

    void illegalInstantiations() {
        // :: error: (optional.collection)
        Optional<List<String>> ols = Optional.of(new ArrayList<String>());
        // :: error: (optional.collection)
        Optional<Set<String>> oss = Optional.of(new HashSet<String>());
    }
}
