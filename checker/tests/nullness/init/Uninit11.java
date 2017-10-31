import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.Unused;

@SubtypeOf({})
@Target(ElementType.TYPE_USE)
@interface DoesNotUseF {}

public class Uninit11 {

    @Unused(when = DoesNotUseF.class)
    public Object f;

    // parameter disambiguate_overloads is just to distinguish the overloaded constructors
    public @DoesNotUseF Uninit11(int disambiguate_overloads) {}

    // :: error: (initialization.fields.uninitialized)
    public Uninit11(boolean disambiguate_overloads) {}

    public Uninit11(long x) {
        f = new Object();
    }
}
