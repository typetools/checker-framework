@SuppressWarnings("all") // Just check for crashes.
public class Issue4852 {
    interface Class1<E extends Class1<E, B>, B extends Class1.Class2<E, B>> {
        abstract class Class2<E extends Class1<E, B>, B extends Class2<E, B>> {}
    }

    class Class3 {
        private void f(Class1<?, ?> x) {}
    }
}
