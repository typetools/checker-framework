import java.io.*;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

class LineNumberReaderTest implements Closeable {

  private final @Owning LineNumberReader reader;

  LineNumberReaderTest(File toRead) {
    LineNumberReader reader = null;
    try {
      reader = new LineNumberReader(new FileReader(toRead));
      this.reader = reader;
      advance();
    } catch (IOException e) {
      if (reader != null) {
        try {
          reader.close();
        } catch (Exception exceptionOnClose) {
          e.addSuppressed(exceptionOnClose);
        }
      }
      throw new RuntimeException(e);
    }
  }

  @RequiresNonNull("reader")
  protected void advance(@UnknownInitialization LineNumberReaderTest this) throws IOException {}

  @Override
  @EnsuresCalledMethods(value = "reader", methods = "close")
  public void close() throws IOException {
    reader.close();
  }
}
