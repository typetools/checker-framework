// Test case for issue #5175: https://github.com/typetools/checker-framework/issues/5175

// The Resource Leak Checker issues the following error:
// LemmaStack.java:40: error: [reset.not.owning] Calling method startProver resets the must-call
// obligations of the expression this, which is non-owning. Either annotate its declaration with an
// @Owning annotation, extract it into a local variable, or write a corresponding
// @CreatesMustCallFor annotation on the method that encloses this statement.
//     startProver();
//                ^
// 1 error

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

@MustCall("close") public class LemmaStack implements Closeable {

    private @Owning @MustCall("close") PrintWriter session;

    @CreatesMustCallFor("this")
    @EnsuresNonNull("session")
    private void startProver() {
        try {
            if (session != null) {
                session.close();
            }
            session = new PrintWriter("filename.txt");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public LemmaStack() {
        startProver();
    }

    @EnsuresCalledMethods(value = "session", methods = "close")
    @Override
    public void close(LemmaStack this) {
        session.close();
    }
}
