import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import checkers.quals.*;
import checkers.nullness.quals.*;

import tests.util.SuperQual;
import tests.util.SubQual;

// This test case is quite meaningless, as it's not run with the
// Nullness Checker. See nullness/UnusedNullness.java instead.
public class UnusedTypes {

  @TypeQualifier
  @SubtypeOf({})
  @Target(ElementType.TYPE_USE)
  public @interface Prototype {}

  @Unused(when=Prototype.class)
  public Object ppt;

  protected @Prototype UnusedTypes() {
    // It should be legal to initialize an unused field to null in the
    // constructor.
    this.ppt = null;
  }

}
