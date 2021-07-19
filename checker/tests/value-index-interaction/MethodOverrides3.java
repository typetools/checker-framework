// This class should not issues any errors, since these annotations are identical to the ones
// on java.io.PrintWriter in the Index JDK.

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;

public class MethodOverrides3 extends PrintWriter {
  public MethodOverrides3(File file) throws FileNotFoundException {
    super(file);
  }

  @Override
  public void write(char[] buf, @IndexFor("#1") int off, @IndexOrHigh("#1") int len) {}
}
