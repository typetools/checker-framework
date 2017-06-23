// @below-java8-jdk-skip-test

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Test cases for two rules:
 *
 * <p>Rule #6: "Avoid using Optional in fields, method parameters, and collections."
 *
 * <p>Rule #7: "Don't use an Optional to wrap any collection type (List, Set, Map). Instead, use an
 * empty collection to represent the absence of values.
 */
public class Marks6 {

    //:: error: (optional.field)
    Optional<String> optionalField = Optional.ofNullable(null);

    //:: error: (optional.parameter)
    void optionalParameter(Optional<String> arg) {}

    Optional<String> okUses() {
        Optional<String> os = Optional.of("hello world");
        return os;
    }

    void illegalInstantiations() {
        //:: error: (optional.as.element.type)
        List<Optional<String>> los = new ArrayList<>();
        //:: error: (optional.as.element.type)
        List<Optional<String>> los2 = new ArrayList<Optional<String>>();
        //:: error: (optional.as.element.type)
        Set<Optional<String>> sos = new HashSet<>();
        //:: error: (optional.collection)
        Optional<List<String>> ols = Optional.of(new ArrayList<String>());
        //:: error: (optional.collection)
        Optional<Set<String>> oss = Optional.of(new HashSet<String>());
    }
}
