package java.lang;
import checkers.javari.quals.*;

import java.util.Iterator;

public interface Iterable<T> {
     @PolyRead Iterator<T> iterator(@PolyRead Iterable<T> this);
}
