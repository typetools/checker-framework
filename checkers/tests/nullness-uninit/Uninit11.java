import checkers.nullness.quals.*;
import checkers.quals.*;
import java.lang.annotation.*;

@TypeQualifier
@SubtypeOf({})
@Target(ElementType.TYPE_USE)
@interface DoesNotUseF {}

public class Uninit11 {

  @Unused(when=DoesNotUseF.class)
  public Object f;

  // parameter x is just to distinguish the overloaded constructors
  public Uninit11(@DoesNotUseF Uninit11 this, int x) {
  }

  public Uninit11(long x) {
    f = new Object();
  }

}

