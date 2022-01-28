class NullnessIssue4996 {
    abstract class CaptureOuter<T> {
        abstract T get();

        abstract class Inner {
            abstract T get();
        }
    }

    class Client {
        Object getFrom(CaptureOuter<?> o) {
            // :: error: (return.type.incompatible)
            return o.get();
        }

        Object getFrom(CaptureOuter<?>.Inner o) {
            // :: error: (return.type.incompatible)
            return o.get();
        }
    }
}
