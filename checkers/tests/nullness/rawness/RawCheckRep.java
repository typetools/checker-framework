import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

// @skip-test -- looks like a bug in the initialization checkers

public class RawCheckRep {

  Object x;

  RawCheckRep() {
    x = "hello";
    checkRep();
  }

  void checkRep(@UnderInitialization(RawCheckRep.class) @Raw(RawCheckRep.class) RawCheckRep this) {
    x.toString();
  }

}
