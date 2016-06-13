// Test case for #692
// https://github.com/typetools/checker-framework/issues/692
public class Issue692<T extends Enum<T>> {
    private Class<T> tClass;

    private boolean method(Object param) {
        Class<?> paramClass = param.getClass();
        return paramClass == tClass || paramClass.getSuperclass() == tClass;
    }
}
