import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;

@InheritableMustCall("close")
class StaticOwningField implements Closeable {

    // Instance field

    private @Owning @MustCall("close") PrintStream ps_instance;

    @CreatesMustCallFor("this")
    void m_instance() throws IOException {
        ps_instance.close();
        ps_instance = new PrintStream("filename.txt");
    }

    @EnsuresCalledMethods(value = "ps_instance", methods = "close")
    @Override
    public void close() {
        ps_instance.close();
    }

    // Static field

    private static @Owning @MustCall("close") PrintStream ps_static;

    static void m_static() throws IOException {
        ps_static.close();
        ps_static = new PrintStream("filename.txt");
    }

    private static @Owning @MustCall("close") PrintStream ps_static_initialized1 =
            newPrintStreamWithoutExceptions();

    private static @Owning @MustCall("close") PrintStream ps_static_initialized2;

    static {
        ps_static_initialized2 = newPrintStreamWithoutExceptions();
    }

    private static final @Owning @MustCall("close") PrintStream ps_static_final_initialized1 =
            newPrintStreamWithoutExceptions();

    private static final @Owning @MustCall("close") PrintStream ps_static_final_initialized2;

    static {
        ps_static_final_initialized2 = newPrintStreamWithoutExceptions();
    }

    public static PrintStream newPrintStreamWithoutExceptions() {
        try {
            return new PrintStream("filename.txt");
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
