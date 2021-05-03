// A test for some issues related to the getFD() method in RandomAccessFile.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;

class FileDescriptors {
  void test(@Owning RandomAccessFile r) throws Exception {
    @MustCall("close") FileDescriptor fd = r.getFD();
    // :: error: assignment
    @MustCall({}) FileDescriptor fd2 = r.getFD();
  }

  void test2(@Owning RandomAccessFile r) throws Exception {
    @MustCall("close") FileInputStream f = new FileInputStream(r.getFD());
    // :: error: assignment
    @MustCall({}) FileInputStream f2 = new FileInputStream(r.getFD());
  }
}
