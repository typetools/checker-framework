import java.io.*;

// @skip-test commented out to avoid breaking the build

class ReadyReadLine {

  void m(BufferedReader buf) throws IOException {
    // ready() is annotated as:  @EnsuresNonNullIf(expression="readLine()", result=true)
    if (buf.ready()) {
      String line = buf.readLine();
      line.toString();
    }
  }

}
