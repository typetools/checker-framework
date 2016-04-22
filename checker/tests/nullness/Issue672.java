// Testcase for Issue 672
// https://github.com/typetools/checker-framework/issues/672
import java.util.concurrent.CompletionException;

final class Issue672 {
    final Throwable ex;

    Issue672(Throwable x) { ex = x; }

    static Issue672 test1(Throwable x) {
        return new Issue672(x instanceof CompletionException ? x
                : new CompletionException(x));
    }
    static Issue672 test2(Throwable x) {
        return test1(x instanceof CompletionException ? x
                : new CompletionException(x));
    }
}
