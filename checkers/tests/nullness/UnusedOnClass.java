import java.lang.annotation.*;
import checkers.quals.*;

public final class UnusedOnClass {
  public static void read_serialized_pptmap2(@MyNonPrototype MyInvariant inv) {
    inv.ppt.toString();
  }
}


@MyPrototype
abstract class MyInvariant {
  @Unused(when=MyPrototype.class)
  public String ppt = "hello";
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf({})
@interface MyPrototype {}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(MyPrototype.class)
@DefaultQualifierInHierarchy
@interface MyNonPrototype {}
