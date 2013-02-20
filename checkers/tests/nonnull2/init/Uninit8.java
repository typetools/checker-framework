import checkers.initialization.quals.Unclassified;
import checkers.nonnull.quals.EnsuresNonNull;
import checkers.nullness.quals.AssertNonNullAfter;
import checkers.nullness.quals.Raw;

public class Uninit8 {

  Object f;

  Uninit8() {
    setFields();
    f.toString();
  }

  @EnsuresNonNull("f")
  void setFields(@Raw @Unclassified Uninit8 this) {
    f = new Object();
  }

}
