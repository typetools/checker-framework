import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Uninit9 {
  public Object f;

  Uninit9() {
    f = new Object();
  }
}

class Uninit9Sub extends Uninit9 {
  Uninit9Sub() {
    super();
    fIsSetOnEntry();
  }

  @RequiresNonNull("f")
  void fIsSetOnEntry(@UnknownInitialization Uninit9Sub this) {}
}
