// Test case for Issue 392:
// https://code.google.com/p/checker-framework/issues/detail?id=392

@SuppressWarnings("javari")
public class Issue392<T> {

    public <T> void getFields(T t) {
        Object o = new Object[] {t,t};
    }
}