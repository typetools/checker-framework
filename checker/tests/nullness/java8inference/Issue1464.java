// Test case for issue 1464
// https://github.com/typetools/checker-framework/issues/1464

public class Issue1464 {

  public interface Variable<T> {

    void addChangedListener(VariableChangedListener<T> listener);
  }

  public interface VariableChangedListener<T> {

    void variableChanged(final Variable<T> variable);
  }

  protected <T> void addChangedListener(
      final Variable<T> variable, final VariableChangedListener<T> listener) {}

  public void main(final Variable<?> tmp) {
    addChangedListener(tmp, variable -> System.out.println(variable));
  }
}
