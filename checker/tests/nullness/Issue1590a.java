// This test case shows that @SuppressWarnings("initialization.") has no effect
@SuppressWarnings("initialization.")
public class Issue1590a {

    private String a;

    public Issue1590a() {
        // :: error: (method.invocation.invalid)
        init();
    }

    public void init() {
        // :: error: (initialization.fields.uninitialized)
        a = "gude";
    }
}
