import checkers.util.report.quals.*;

class Accesses {
    class Demo {
        @ReportReadWrite
        Object read;

        @ReportWrite
        Object write;

        @ReportCall
        Object foo(Object p) { return null; }

        void implicitRead() {
            //:: error: (fieldreadwrite)
            Object o = read;
            // A read counts as access
            //:: error: (fieldreadwrite)
            read = null;
            //:: error: (fieldreadwrite)
            read.toString();
        }

        void implicitWrite() {
            Object o = write;
            //:: error: (fieldwrite)
            write = null;
            write.toString();
        }

        void implicitMethod() {
            //:: error: (methodcall)
            foo(null);
            //:: error: (methodcall)
            equals(foo(null));
        }
    }

    void accessesRead(Demo d) {
        //:: error: (fieldreadwrite)
        Object o = d.read;
        // A read counts as access
        //:: error: (fieldreadwrite)
        d.read = null;
        //:: error: (fieldreadwrite)
        d.read.toString();
    }

    void accessesWrite(Demo d) {
        Object o = d.write;
        //:: error: (fieldwrite)
        d.write = null;
        d.write.toString();
    }

    void accessesMethod(Demo d) {
        //:: error: (methodcall)
        d.foo(null);
        //:: error: (methodcall)
        d.equals(d.foo(null));
    }
}