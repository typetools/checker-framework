// A test that the checker doesn't ask you to close System.in, System.out, or System.err.

import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;
import java.util.Scanner;

class SystemInOut {
    void test() {
        @MustCall({}) InputStream in = System.in;
        @MustCall({}) OutputStream out = System.out;
        @MustCall({}) OutputStream err = System.err;
        @MustCall({}) Scanner sysIn = new Scanner(System.in);
    }
}
