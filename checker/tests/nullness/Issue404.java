import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: inference problem with dependencies between multiple methods.
// Eventually, the test should work when executed on >= Java 8.
//@skip-test
//@below-java8-jdk-skip-test
public final class Issue404 {
    public Set<String> uniqueTrimmed(final Collection<String> strings) {
        return strings.stream().map(String::trim).collect(Collectors.toSet());
    }
}
