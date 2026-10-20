import java.io.*;
import java.util.*;

public class PurgeTxnLog {

  public static void purge() throws IOException {
    staticMethod();
  }

  static void staticMethod() {

    class MyFileFilter implements FileFilter {
      MyFileFilter() {}

      public boolean accept(File f) {
        return true;
      }
    }

    MyFileFilter m = new MyFileFilter();
  }
}
