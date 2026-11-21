// Test input for ResourceLeakCountMustCallTest.

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;

class IndependentResets {
  static native Socket open();

  @CreatesMustCallFor("#1")
  static native void reset(Socket socket);

  static native void use(
      Socket s0,
      Socket s1,
      Socket s2,
      Socket s3,
      Socket s4,
      Socket s5,
      Socket s6,
      Socket s7,
      Socket s8,
      Socket s9,
      Socket s10,
      Socket s11,
      Socket s12,
      Socket s13,
      Socket s14,
      Socket s15);

  static void leak(boolean[] reset) {
    // Every call to open() and reset() is counted, and every obligation leaks.
    Socket s0 = open();
    Socket s1 = open();
    Socket s2 = open();
    Socket s3 = open();
    Socket s4 = open();
    Socket s5 = open();
    Socket s6 = open();
    Socket s7 = open();
    Socket s8 = open();
    Socket s9 = open();
    Socket s10 = open();
    Socket s11 = open();
    Socket s12 = open();
    Socket s13 = open();
    Socket s14 = open();
    Socket s15 = open();
    if (reset[0]) {
      reset(s0);
    }
    if (reset[1]) {
      reset(s1);
    }
    if (reset[2]) {
      reset(s2);
    }
    if (reset[3]) {
      reset(s3);
    }
    if (reset[4]) {
      reset(s4);
    }
    if (reset[5]) {
      reset(s5);
    }
    if (reset[6]) {
      reset(s6);
    }
    if (reset[7]) {
      reset(s7);
    }
    if (reset[8]) {
      reset(s8);
    }
    if (reset[9]) {
      reset(s9);
    }
    if (reset[10]) {
      reset(s10);
    }
    if (reset[11]) {
      reset(s11);
    }
    if (reset[12]) {
      reset(s12);
    }
    if (reset[13]) {
      reset(s13);
    }
    if (reset[14]) {
      reset(s14);
    }
    if (reset[15]) {
      reset(s15);
    }
    use(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15);
  }
}
