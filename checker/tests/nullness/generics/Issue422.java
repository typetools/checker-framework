public class Issue422 {
  public <T> boolean f(T newValue, T oldValue) {
    return (oldValue instanceof Boolean || oldValue instanceof Integer)
        && oldValue.equals(newValue);
  }
}
