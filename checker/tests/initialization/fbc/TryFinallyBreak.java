// Test case for Issue 548:
// https://github.com/typetools/checker-framework/issues/548
// @skip-test

class TryFinallyBreak {
    String foo() {
        String ans = "x";
        while (this.hashCode() > 10000) {
            try {
                // empty body
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String bar() {
        String ans = "x";
        while (true) {
            try {
                // Note the additional break;
                break;
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }
}
