import java.util.*;

class FullyQualifiedAnnotation {

    void client1(Iterator i) {
        @SuppressWarnings("nullness")
        @checkers.nullness.quals.NonNull Object handle2 = i.next();
        handle2.toString();
    }

    void client2(Iterator i) {
        @SuppressWarnings("nullness")
        @checkers.nullness.quals.NonNull Object handle2 = i.next();
        handle2.toString();
    }

    void client3(Iterator<Object> i) {
        @SuppressWarnings("nullness")
        @checkers.nullness.quals.NonNull Object handle2 = i.next();
        handle2.toString();
    }

    void client4(Iterator<Object> i) {
        @SuppressWarnings("nullness")
        @checkers.nullness.quals.NonNull Object handle2 = i.next();
        handle2.toString();
    }

}
