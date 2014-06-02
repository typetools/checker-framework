import org.checkerframework.checker.igj.qual.*;
import java.util.Collection;

@I
public class IResolution {
    public static <T> @I Collection<T> forIterable(final @I Iterable<T> iterable)
    {
        @I Collection<T> col = (Collection<T>)iterable;
        return col;
    }
}
