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

  protected UnusedNullness() @Prototype {
      // It should be legal to initialize an unused field to null in
      // a constructor with @Prototype receiver.
      this.ppt = null;
  }

  protected void protometh() @Prototype {
      // It should be legal to initialize the unused field to null in
      // a method with @Prototype receiver.
      this.ppt = null;
  }

  protected void meth() {
      // Otherwise it's not legal.
      //:: error: (assignment.type.incompatible)
      this.ppt = null;
  }
}
