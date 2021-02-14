@SuppressWarnings("all") // Check for crashes.
public class AnonymousFieldAccess {
    static class SomeClass {
        Object fieldInSomeClass;
    }

    void createTreeAnnotator() {
        new SomeClass() {
            Object f = fieldInSomeClass;
        };
    }
}
