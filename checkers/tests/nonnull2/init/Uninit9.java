import checkers.initialization.quals.Unclassified;
import checkers.nonnull.quals.RequiresNonNull;
import checkers.nullness.quals.*;

public class Uninit9 {
  public Object f;
  Uninit9() { f = new Object(); }
}

class Uninit9Sub extends Uninit9 {
  Uninit9Sub() {
    super();
    fIsSetOnEntry();
  }
  @RequiresNonNull("f")
  void fIsSetOnEntry(@Raw @Unclassified Uninit9Sub this) {
  }
}
