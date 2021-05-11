// A test case for a crash while checking hfds.

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class TryWithResourcesCrash {
  void test(FileSystem fs, byte[] bytes, String path) throws IOException {
    try (FSDataOutputStream out = fs.createFile(path).overwrite(true).build()) {
      out.write(bytes);
    }
  }

  class FSDataOutputStream extends DataOutputStream {
    FSDataOutputStream(OutputStream os) {
      super(os);
    }
  }

  abstract class FSDataOutputStreamBuilder<
      S extends FSDataOutputStream, B extends FSDataOutputStreamBuilder<S, B>> {
    abstract S build();

    abstract B overwrite(boolean b);
  }

  abstract class FileSystem implements Closeable {
    abstract FSDataOutputStreamBuilder createFile(String s);
  }
}
