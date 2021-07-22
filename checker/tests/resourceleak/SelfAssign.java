// test assignments of the same variable to itself

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

class SelfAssign {

    static void test0() throws IOException {
        InputStream selfAssignIn0 = new FileInputStream("file.txt");
        try {
            selfAssignIn0 = selfAssignIn0;
        } finally {
            selfAssignIn0.close();
        }
    }

    static void test1(boolean b) throws IOException {
        InputStream selfAssignIn = new FileInputStream("file.txt");
        try {
            selfAssignIn =
                    selfAssignIn.markSupported()
                            ? selfAssignIn
                            : new BufferedInputStream(selfAssignIn);
        } finally {
            selfAssignIn.close();
        }
    }

    static void test2(boolean b) throws IOException {
        InputStream in = new FileInputStream("file.txt");
        try {
            in = new BufferedInputStream(in);
        } finally {
            in.close();
        }
    }
}
