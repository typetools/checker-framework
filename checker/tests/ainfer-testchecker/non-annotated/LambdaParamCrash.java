// A test case that caused an assertion to be violated when inferring something
// about e1, the parameter of the lambda. This test case is based on a crash
// encountered in the wild.

import java.io.IOException;
import java.util.function.BiConsumer;

@SuppressWarnings("all") // only checking for crashes
public class LambdaParamCrash {

    void groupAndSend() {
        addListener(
                (r, e1) -> {
                    if (e1 == null) {
                        e1 = badResponse();
                    }
                });
    }

    private IOException badResponse() {
        return new IOException();
    }

    public static <T> void addListener(BiConsumer<? super T, ? super Throwable> action) {}
}
