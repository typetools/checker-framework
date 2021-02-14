// Test case for Issue 765
// https://github.com/typetools/checker-framework/issues/765
public class Issue765 {
    Thread thread = new Thread() {};

    void execute() {
        thread =
                new Thread() {
                    @Override
                    public void run() {}
                };
        thread.start();
    }
}
