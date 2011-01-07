package tests;

import java.util.*;
import javax.tools.*;

import checkers.quals.*;
import checkers.javari.quals.*;

public class TestRun implements Iterable<Diagnostic<? extends JavaFileObject>> {

    private final boolean result;
    private final String output;
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

    TestRun(/*@PolyRead*/ Boolean result, /*@PolyRead*/ String output,
            /*@PolyRead*/ List<Diagnostic<? extends JavaFileObject>> diagnostics) /*@PolyRead*/ {
       this.result = result.booleanValue();
       this.output = output;
       this.diagnostics = Collections.unmodifiableList(diagnostics);
    }

    public boolean getResult() /*@ReadOnly*/ {
        return result;
    }

    public /*@PolyRead*/ String getOutput() /*@PolyRead*/ {
        return output;
    }

    public /*@PolyRead*/ List<Diagnostic<? extends JavaFileObject>> getDiagnostics() /*@PolyRead*/ {
        return diagnostics;
    }

    public /*@PolyRead*/ Iterator<Diagnostic<? extends JavaFileObject>> iterator() /*@PolyRead*/ {
        return diagnostics.iterator();
    }
}
