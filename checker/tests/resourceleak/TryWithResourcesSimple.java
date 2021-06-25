// A simple test that a try-with-resources socket doesn't issue a false positive.

import java.io.*;
import java.net.Socket;
import java.nio.channels.*;
import java.util.*;

class TryWithResourcesSimple {
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
}
