import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MyDataInputStream extends DataInputStream {
  public MyDataInputStream(InputStream in) {
    super(in);
  }

  public void readFully(long position, ByteBuffer buf) throws IOException {
    if (in instanceof ByteBufferPositionedReadable) {
      ((ByteBufferPositionedReadable) in).readFully(position, buf);
    } else {
      throw new UnsupportedOperationException(
          "Byte-buffer pread " + "unsupported by " + in.getClass().getCanonicalName());
    }
  }

  interface ByteBufferPositionedReadable {
    void readFully(long position, ByteBuffer buf) throws IOException;
  }
}
