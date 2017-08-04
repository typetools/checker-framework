// Test case for Issue 1415.
// https://github.com/typetools/checker-framework/issues/1415
// @below-java8-jdk-skip-test

@SuppressWarnings("") // Check for crashes.
class Issue1415 {
    static class Optional<T> {
        static <T> Optional<T> absent() {
            return null;
        }

        static <T> Optional<T> of(T p) {
            return null;
        }
    }

    static class Box<T> {
        void box(T p) {}
    }

    static class Crash9 {
        <T extends Enum<T>> void foo(boolean b, Box<Optional<T>> box, Class<T> enumClass) {
            box.box(b ? Optional.<T>absent() : Optional.of(Enum.valueOf(enumClass, "hi")));
        }
    }
}
