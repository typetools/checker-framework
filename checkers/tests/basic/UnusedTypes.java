import checkers.quals.*;
import checkers.nullness.quals.*;

import tests.util.SuperQual;
import tests.util.SubQual;

public class UnusedTypes {

  @TypeQualifier
  @SubtypeOf({})
  public @interface Prototype {}

  @Unused(when=Prototype.class)
  public Object ppt;

  protected @Prototype UnusedTypes() {
    // It should be legal to initialize an unused field to null in the
    // constructor.
    this.ppt = null;
  }

}
