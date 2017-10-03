// Test case for Issue 1546
// https://github.com/typetools/checker-framework/issues/1546
@SuppressWarnings("") // check for crashes
class Issue1546 {

    <T> void m(T t) {}

    {
        try {
            new Runnable() {
                public void run() {}
            };
        } finally {
            m("Hi");
        }
    }
}
