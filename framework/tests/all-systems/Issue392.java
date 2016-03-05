// Test case for Issue 392:
// https://github.com/typetools/checker-framework/issues/392

@SuppressWarnings("javari")
public class Issue392<T> {

    public <T> void getFields(T t) {
        Object o = new Object[] { t, t };
    }
}
