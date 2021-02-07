import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ReceiverAnnotation {

    void receiver1(ReceiverAnnotation this) {}

    void receiver2(@NonNull ReceiverAnnotation this) {}

    void receiver3(@Nullable ReceiverAnnotation this) {}
}
