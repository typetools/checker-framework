/**
 * Demonstrates an issue in the Checker Framework with handling the nearest enclosing element for
 * temporary variable declarations, leading to a crash during analysis.
 */
@SuppressWarnings("all") // only check for crashes
public abstract class CrashForTempVar<T extends Number> {

  private final CrashForTempVar<T> _base;

  protected CrashForTempVar(final CrashForTempVar<T> base) {
    _base = base;
  }

  public T getValue() {
    return _base.getValue();
  }

  protected CrashForTempVar<T> getBase() {
    return _base;
  }

  protected abstract boolean evaluateLayer(final T baseValue, final T testValue);

  public boolean evaluate(final T testValue) {
    return evaluateLayer(getBase().getValue(), testValue);
  }
}
