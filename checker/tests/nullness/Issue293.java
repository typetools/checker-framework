// Test for Issue 293:
// https://github.com/typetools/checker-framework/issues/293

public class Issue293 {
    void test1() {
        String s;
        try {
            s = read();
        } catch (Exception e) {
            // Because of definite assignment, s cannot be mentioned here.
            write("Catch.");
            return;
        } finally {
            // Because of definite assignment, s cannot be mentioned here.
            write("Finally.");
        }

        // s is definitely initialized here.
        write(s);
    }

    void test2() {
        String s2 = "";
        try {
        } finally {
            write(s2);
        }
    }

    void test3() throws Exception {
        String s = "";
        try {
            throw new Exception();
        } finally {
            write(s);
        }
    }

    void test4() throws Exception {
        String s = "";
        try {
            if (true) {
                throw new Exception();
            } else {
                s = null;
            }
        } finally {
            // :: error: argument.type.incompatible
            write(s);
        }
    }

    String read() throws Exception {
        throw new Exception();
    }

    void write(String p) {
        System.out.println(p);
    }
}
