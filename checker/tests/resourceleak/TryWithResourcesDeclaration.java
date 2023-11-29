// Tests for try-with-resources that declare local variables in the resource declaration.

import java.io.*;
import java.net.Socket;
import java.nio.channels.*;
import java.util.*;

class TryWithResourcesDeclaration {
  static void test(String address, int port) {
    try (Socket socket = new Socket(address, port)) {

    } catch (Exception e) {

    }
  }

  public boolean isPreUpgradableLayout(File oldF) throws IOException {

    if (!oldF.exists()) {
      return false;
    }
    // check the layout version inside the storage file
    // Lock and Read old storage file
    try (RandomAccessFile oldFile = new RandomAccessFile(oldF, "rws");
        FileLock oldLock = oldFile.getChannel().tryLock()) {
      if (null == oldLock) {
        throw new OverlappingFileLockException();
      }
      oldFile.seek(0);
      int oldVersion = oldFile.readInt();
      return false;
    }
  }

  public void testNestedTryWithResourcesDecls(Properties prop, ClassLoader cl, String propfile)
      throws Exception {
    try (InputStream in = cl.getResourceAsStream(propfile)) {
      try (InputStream fis = new FileInputStream(propfile)) {
        prop.load(fis);
      }
    }
  }
}
