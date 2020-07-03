import java.util.Optional;
import java.util.stream.Stream;
import org.checkerframework.checker.linear.qual.Linear;

public class LinearTest {

    void Test() {
        @SuppressWarnings("assignment.type.incompatible")
        @Linear
        Stream<String> stringStream = Stream.of("A", "B", "C", "D");
        Stream<String> a =
                stringStream; // Assignment doesn't use up variables, hence, stringStream is still
        // linear
        check(stringStream); // Passing stringStream as an argument doesn't use it up, hence, it
        // is still linear

        Optional<String> result1 =
                stringStream
                        .findAny(); // Due to the method invocation, stream is used up and is now
        // unusable
        System.out.println(result1.get());
        // Since stringStream is unusable now, it can't be used for assignment, method invocation or
        // as an argument

        // :: error: (use.unsafe)
        Optional<String> result2 = stringStream.findFirst();

        // :: error: (use.unsafe)
        check(stringStream);
    }

    // Method to check whether string stream s can be passed as an argument
    void check(Stream<String> s) {}
}
