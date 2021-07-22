import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;
import java.io.IOException;
import java.net.*;

public class FileDescriptorTest {
    // This is the original test case. It fails because `in.close()` might throw an exception, which
    // is
    // not caught; therefore, file might still be open.
    public static void readPropertiesFile(File from) throws IOException {
        // This is a false positive.
        // :: error: required.method.not.called
        RandomAccessFile file = new RandomAccessFile(from, "rws");
        FileInputStream in = null;
        try {
            in = new FileInputStream(file.getFD());
            file.seek(0);
        } finally {
            if (in != null) {
                in.close();
            }
            file.close();
        }
    }

    // This is a similar test to the above, but without using the indirection through getFD().
    // This test case demonstrates that the problem is not related to getFD().
    // This warning is a false positive, and should be resolved at the same time as the warning
    // above.
    // :: error: required.method.not.called
    public static void sameScenario_noFD(@Owning Socket sock) throws IOException {
        InputStream in = null;
        try {
            in = sock.getInputStream();
        } finally {
            if (in != null) {
                in.close();
            }
            sock.close();
        }
    }

    // This version, written by Narges, does not issue a false positive.
    public static void readPropertiesFile_noFP(File from) throws IOException {
        RandomAccessFile file = new RandomAccessFile(from, "rws");
        FileInputStream in = null;
        try {
            in = new FileInputStream(file.getFD());
            in.close();
        } catch (IOException e) {
            file.close();
        }
    }
}
