// https://github.com/eisop/checker-framework/issues/300
// The receiver could be nullable after an invocation.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue300 {
    class Bug {
        void setFieldNull(Bug b) {
            EisopIssue300.this.currentNode = null;
        }
    }

    @Nullable Bug currentNode = new Bug();

    void test() {
        if (currentNode == null) {
            return;
        }
        currentNode.setFieldNull(currentNode);
        // :: error: (dereference.of.nullable)
        currentNode.toString();
    }

    public static void main(String[] args) {
        new EisopIssue300().test();
    }
}
