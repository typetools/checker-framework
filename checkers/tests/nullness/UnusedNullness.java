import java.lang.annotation.*;

import checkers.quals.*;
import checkers.nullness.quals.*;

import tests.util.SuperQual;
import tests.util.SubQual;

public class UnusedNullness {

  @TypeQualifier
  @SubtypeOf({})
  @Target(ElementType.TYPE_USE)
  public @interface Prototype {}

  @Unused(when=Prototype.class)
  public Object ppt;

  protected @Prototype UnusedNullness() {
      // It should be legal to initialize an unused field to null in
      // a constructor with @Prototype receiver.
      this.ppt = null;
  }

  protected @Prototype UnusedNullness(int param) {
      // It should be legal to NOT initialize an unused field in
      // a constructor with @Prototype receiver.
  }

  protected void protometh(@Prototype UnusedNullness this) {
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
