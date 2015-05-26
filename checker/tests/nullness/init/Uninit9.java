import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.checker.nullness.qual.*;

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
  void fIsSetOnEntry(@Raw @UnknownInitialization Uninit9Sub this) {
  }
}
