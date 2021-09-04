// A test case for https://github.com/typetools/checker-framework/issues/4838.
//
// This test that shows that no unsoundess occurs when a single close() method is responsible
// for closing two resources.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.IOException;
import java.net.Socket;

@MustCall("dispose") class TwoResourcesECM {
    @Owning Socket s1, s2;

    // The contracts.postcondition error below is thrown because s1 is not final,
    // and therefore might theoretically be side-effected by the call to s2.close()
    // even on the non-exceptional path. See ReplicaInputStreams.java for a variant
    // of this test where such an error is not issued. Because this method can leak
    // along both regular and exceptional exits, both errors are issued.
    @EnsuresCalledMethods(
            value = {"this.s1", "this.s2"},
            methods = {"close"})
    // :: error: contracts.postcondition :: error: destructor.exceptional.postcondition
    public void dispose() throws IOException {
        s1.close();
        s2.close();
    }

    static void test1(TwoResourcesECM obj) {
        try {
            obj.dispose();
        } catch (IOException ioe) {

        }
    }
}
