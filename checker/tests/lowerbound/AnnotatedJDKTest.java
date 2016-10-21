import java.io.PrintWriter;
import org.checkerframework.checker.lowerbound.qual.*;

public class AnnotatedJDKTest {

    public void printWriterWrite(PrintWriter writer) {
        //:: error: (argument.type.incompatible)
        writer.write(-1);

        writer.write(8);
    }
}
