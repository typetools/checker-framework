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

    public static String join(String delimiter, Object[] values) {
        return join(delimiter, Arrays.asList(values));
    }

    public static String join(String delimiter, Iterable<?> values) {
        StringBuilder sb = new StringBuilder();

        boolean isntFirst = false;
        for (Object value : values) {
            if (isntFirst)
                sb.append(delimiter);
            sb.append(value);
            isntFirst = true;
        }

        return sb.toString();
    }
}
