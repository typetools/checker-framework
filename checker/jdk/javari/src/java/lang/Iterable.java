package java.lang;
import org.checkerframework.checker.javari.qual.*;

import java.util.Iterator;

public interface Iterable<T> {
     @PolyRead Iterator<T> iterator(@PolyRead Iterable<T> this);
}
