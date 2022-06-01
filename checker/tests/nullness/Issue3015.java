// Test case for issue #3015:
// https://github.com/typetools/checker-framework/issues/3015

class Issue3015 {
    void acquire() {
        String s = "";

        try {
            return;
        } finally {
            try {
                signal();
            } finally {
                s.toString();
            }
        }
    }

    void signal() {}
}
