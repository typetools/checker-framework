import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

import java.io.IOException;
import java.io.PrintStream;

public class TypeProcessError {

    @SuppressWarnings("required.method.not.called")
    @Owning
    @MustCall("close") PrintStream ps_instance;

    @SuppressWarnings("required.method.not.called")
    private static @Owning @MustCall("close") PrintStream ps_static;

    @SuppressWarnings("missing.creates.mustcall.for")
    static void m_static() throws IOException {
        ps_static.close();
        ps_static = new PrintStream("filename.txt");
    }
}

class TypeProcessError2 extends TypeProcessError {
    @SuppressWarnings("required.method.not.called")
    @Owning
    @MustCall("close") PrintStream ps_instance;

    @SuppressWarnings("missing.creates.mustcall.for")
    void m() throws IOException {
        super.ps_instance.close();
        super.ps_instance = new PrintStream("filename.txt");
    }
}
