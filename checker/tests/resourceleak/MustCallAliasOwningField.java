// Based on a MustCallAlias scenario in Zookeeper.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

public @MustCall("shutdown") class MustCallAliasOwningField {

    private final @Owning BufferedInputStream input;

    public MustCallAliasOwningField(@Owning BufferedInputStream input, boolean b) {
        this.input = input;
        if (b) {
            DataInputStream d = new DataInputStream(input);
            authenticate(d);
        }
    }

    @EnsuresCalledMethods(value = "this.input", methods = "close")
    public void shutdown() throws IOException {
        input.close();
    }

    public static void authenticate(InputStream is) {}

    public void wrapField() {
        DataInputStream dis = new DataInputStream(input);
    }
}
