// A test that the checker doesn't ask you to close System.in, System.out, or System.err.

import java.io.*;
import java.util.Scanner;
import org.checkerframework.checker.mustcall.qual.*;

class SystemInOut {
  void test() {
    @MustCall({}) InputStream in = System.in;
    @MustCall({}) OutputStream out = System.out;
    @MustCall({}) OutputStream err = System.err;
    @MustCall({}) Scanner sysIn = new Scanner(System.in);
  }
}
