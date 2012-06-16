package checkers.util.test;

import java.util.*;
import javax.tools.*;

import checkers.quals.*;
import checkers.javari.quals.*;

public class TestRun implements Iterable<Diagnostic<? extends JavaFileObject>> {

    private final boolean result;
    private final String output;
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

    TestRun(/*@PolyRead*/ Boolean result, /*@PolyRead*/ String output,
            /*@PolyRead*/ List<Diagnostic<? extends JavaFileObject>> diagnostics) {
       this.result = result.booleanValue();
       this.output = output;
       this.diagnostics = Collections.unmodifiableList(diagnostics);
    }

    public boolean getResult(/*>>> @ReadOnly TestRun this*/) {
        return result;
    }

    public /*@PolyRead*/ String getOutput(/*>>> @PolyRead TestRun this*/) {
        return output;
    }

    public /*@PolyRead*/ List<Diagnostic<? extends JavaFileObject>> getDiagnostics(/*>>> @PolyRead TestRun this*/) {
        return diagnostics;
    }

    public /*@PolyRead*/ Iterator<Diagnostic<? extends JavaFileObject>> iterator(/*>>> @PolyRead TestRun this*/) {
        return diagnostics.iterator();
    }
}
