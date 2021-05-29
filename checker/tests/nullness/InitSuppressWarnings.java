import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class InitSuppressWarnings {

  private void init_vars(@UnderInitialization(Object.class) InitSuppressWarnings this) {
    @SuppressWarnings({"nullness"})
    @Initialized InitSuppressWarnings initializedThis = this;
  }
}
