import checkers.initialization.quals.*;
import checkers.nullness.quals.*;

public class Uninit8 {

  Object f;

  Uninit8() { setFields(); }

  @EnsuresNonNull("f")
  void setFields(@Raw @Unclassified Uninit8 this) {
    f = new Object();
  }

}
