import java.io.*;
import java.util.*;

public class PurgeTxnLog {

  public static void purge(File dataDir, File snapDir, int num) throws IOException {
    staticMethod();
  }

  static void staticMethod() {

    class MyFileFilter implements FileFilter {

      //      private final String prefix;
      MyFileFilter() {}

      public boolean accept(File f) {
        return true;
      }
    }

    MyFileFilter m = new MyFileFilter();
  }
}
