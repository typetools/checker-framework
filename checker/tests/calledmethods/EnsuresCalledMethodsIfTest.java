// Test case for https://github.com/typetools/checker-framework/issues/4699

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsIf;

import java.io.IOException;

class EnsuresCalledMethodsIfTest {

    @EnsuresCalledMethods(value = "#1", methods = "close")
    // If `sock` is null, `sock.close()` will not be called, and the method will exit normally, as
    // the
    // NullPointerException is caught.  But, the Called Methods Checker
    // assumes the program is free of NullPointerExceptions, delegating verification of that
    // property to the Nullness Checker.  So, the postcondition is verified.
    public static void closeSock(EnsuresCalledMethodsIfTest sock) throws Exception {
        if (!sock.isOpen()) {
            return;
        }
        try {
            sock.close();
        } catch (Exception e) {
        }
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    public static void closeSockOK(EnsuresCalledMethodsIfTest sock) throws Exception {
        if (!sock.isOpen()) {
            return;
        }
        try {
            sock.close();
        } catch (IOException e) {
        }
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    public static void closeSockOK1(EnsuresCalledMethodsIfTest sock) throws Exception {
        if (!sock.isOpen()) {
            return;
        }
        sock.close();
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    public static void closeSockOK2(EnsuresCalledMethodsIfTest sock) throws Exception {
        if (sock.isOpen()) {
            sock.close();
        }
    }

    void close() throws IOException {}

    @SuppressWarnings(
            "calledmethods") // like the JDK's isOpen methods; makes this test case self-contained
    @EnsuresCalledMethodsIf(
            expression = "this",
            result = false,
            methods = {"close"})
    boolean isOpen() {
        return true;
    }
}
