import java.util.Optional;
import java.util.stream.Stream;
import org.checkerframework.checker.linear.qual.Linear;

public class JavaStreamTest {

    @SuppressWarnings("type.argument.type.incompatible")
    void Test() {
        @Linear Stream<String> stringStream = getLinearString();

        Optional<String> result1 = stringStream.findAny();
        // Due to the method invocation, stream is used up and is now unusable

        System.out.println(result1.get());
        // Since stringStream is unusable now, it can't be used for assignment, method invocation or
        // as an argument

        // :: error: (use.unsafe)
        Optional<String> result2 = stringStream.findFirst();
    }

    @SuppressWarnings("return.type.incompatible")
    static @Linear Stream<String> getLinearString() {
        return Stream.of("A", "B", "C", "D");
    }
}
