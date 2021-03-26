// Test case for #692
// https://github.com/typetools/checker-framework/issues/692
public class Issue692<T extends Enum<T>> {

  private boolean method(Object param, Class<T> tClass) {
    Class<?> paramClass = param.getClass();
    return paramClass == tClass || paramClass.getSuperclass() == tClass;
  }
}
