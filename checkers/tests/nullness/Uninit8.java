import checkers.initialization.quals.*;
import checkers.nullness.quals.*;

public class Uninit8 {

  Object f;

  Uninit8() { setFields(); }

  @EnsuresNonNull("f")
  void setFields(@Raw @UnkownInitialization Uninit8 this) {
    f = new Object();
  }

}
