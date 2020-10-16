// Test case for issue #3776: https://tinyurl.com/cfissue/3776

// @skip-test until the bug is fixed

public class Issue3776 {
    void m(WindowFn windowFn) {
        windowFn.new MergeContext() {};
    }
}

class WindowFn {
    class MergeContext {}
}
