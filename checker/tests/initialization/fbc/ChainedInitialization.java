import org.checkerframework.checker.nullness.qual.NonNull;

public class ChainedInitialization {

    @NonNull String f;
    @NonNull String g = f = "hello";

    // Adding this empty initializer suppresses the warning.
    //     {}

    // Adding this constructor does not suppress the warning.
    // ChainedInitialization() {}

}
