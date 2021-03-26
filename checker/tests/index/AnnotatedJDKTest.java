import java.io.PrintWriter;

public class AnnotatedJDKTest {

  public void printWriterWrite(PrintWriter writer) {
    writer.write(-1);

    writer.write(8);
  }
}
