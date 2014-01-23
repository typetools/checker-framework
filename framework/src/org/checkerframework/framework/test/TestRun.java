package org.checkerframework.framework.test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/*>>>
import org.checkerframework.framework.qual.*;
import org.checkerframework.checker.javari.qual.*;
*/

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

    @Override
    public /*@PolyRead*/ Iterator<Diagnostic<? extends JavaFileObject>> iterator(/*>>> @PolyRead TestRun this*/) {
        return diagnostics.iterator();
    }
}
