import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ReceiverAnnotation {

    void receiver1(ReceiverAnnotation this) {}

    // :: error: (nullness.on.receiver)
    void receiver2(@NonNull ReceiverAnnotation this) {}

    // The "type.invalid.annotations.on.use" error message wording is a bit weird, but I think it's
    // clear enough together with the "nullness.on.receiver" message.
    // :: error: (nullness.on.receiver)
    // :: error: (type.invalid.annotations.on.use)
    void receiver3(@Nullable ReceiverAnnotation this) {}
}
