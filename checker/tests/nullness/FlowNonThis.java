import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

import java.io.*;

public class FlowNonThis {

    @Nullable String c;

    public static void main(String[] args) {
        FlowNonThis t = new FlowNonThis();
        t.setup();
        System.out.println(t.c.length());
        t.erase();
        // :: error: (dereference.of.nullable)
        System.out.println(t.c.length());
    }

    public void setupThenErase() {
        setup();
        System.out.println(c.length());
        erase();
        // :: error: (dereference.of.nullable)
        System.out.println(c.length());
    }

    public void justErase() {
        // :: error: (dereference.of.nullable)
        System.out.println(c.length());
        erase();
        // :: error: (dereference.of.nullable)
        System.out.println(c.length());
    }

    @EnsuresNonNull("c")
    public void setup() {
        c = "setup";
    }

    public void erase() {
        c = null;
    }
}
