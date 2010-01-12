import checkers.quals.*;
import checkers.nullness.quals.*;

import checkers.util.test.SuperQual;
import checkers.util.test.SubQual;

public class UnusedNullness {

  @TypeQualifier
  @SubtypeOf({})
  public @interface Prototype {}

  @Unused(when=Prototype.class)
  public Object ppt;

  protected @Prototype UnusedNullness() {
    // It should be legal to initialize an unused field to null in the
    // constructor.
    this.ppt = null;
  }

}
