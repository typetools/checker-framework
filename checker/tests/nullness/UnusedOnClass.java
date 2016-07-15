import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

public final class UnusedOnClass {
    public static void read_serialized_pptmap2(@MyNonPrototype MyInvariant2 inv) {
        inv.ppt.toString();
    }
}

@MyPrototype
abstract class MyInvariant2 {
    @Unused(when = MyPrototype.class)
    public String ppt = "hello";
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@interface MyPrototype {}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(MyPrototype.class)
@DefaultQualifierInHierarchy
@interface MyNonPrototype {}
