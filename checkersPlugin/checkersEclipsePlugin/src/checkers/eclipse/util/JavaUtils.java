package checkers.eclipse.util;

import java.util.*;

public class JavaUtils {
    private JavaUtils() {
        throw new AssertionError("Shouldn't be instantiated");
    }

    public static <T> Iterable<T> iterable(final Iterator<T> iterator) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return iterator;
            }
        };
    }

}
