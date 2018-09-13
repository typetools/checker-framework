import java.io.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestMainParams {
    // :: error: (invalid.annotation.on.parameter)
    public static void main(String @OrderNonDet [] args) {}
}
