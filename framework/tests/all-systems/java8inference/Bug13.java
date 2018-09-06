package inference.guava;

import java.util.Spliterator;
import java.util.stream.Stream;

@SuppressWarnings("") // Just check for crashes.
public class Bug13 {

    public static class MyClass<X> extends MySuperClass<X> {
        static <Z> Stream<Z> stream(MyClass<Z> s, Iterable<Z> iterable) {
            throw new RuntimeException();
        }

        public void method(final Iterable<X> iterable) {
            Spliterator<X> x = Stream.generate(() -> iterable).flatMap(super::stream).spliterator();
        }
    }

    public static class MySuperClass<Z> {
        Stream<Z> stream(Iterable<Z> iterable) {
            throw new RuntimeException();
        }

        static <Z> Stream<Z> stream(MySuperClass<Z> s, Iterable<Z> iterable) {
            throw new RuntimeException();
        }
    }

    public static <Q> void method(final Iterable<Q> iterable, MyClass<Q> myClass) {
        Spliterator<Q> x = Stream.generate(() -> iterable).flatMap(myClass::stream).spliterator();
    }

    public static <Z> Stream<Z> stream(Iterable<Z> iterable) {
        throw new RuntimeException();
    }
}
