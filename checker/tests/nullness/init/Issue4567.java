import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue4567 {

  public Issue4567() {
    this(null);
  }

  protected Issue4567(final @UnderInitialization @Nullable Object variableScope) {}
}
