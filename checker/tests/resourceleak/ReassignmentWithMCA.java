import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;
import java.security.*;

public class ReassignmentWithMCA {
    void testReassignment(File newFile, MessageDigest digester) throws IOException {
        FileOutputStream fout = new FileOutputStream(newFile);
        DigestOutputStream fos = new DigestOutputStream(fout, digester);
        DataOutputStream out = new DataOutputStream(fos);
        try {
            out = new DataOutputStream(new BufferedOutputStream(fos));
            fout.getChannel();
        } finally {
            out.close();
        }
    }

    void testReassignmentWithoutMCA(
            @Owning FileOutputStream fout1, @Owning FileOutputStream fout2, MessageDigest digester)
            throws IOException {
        DigestOutputStream fos1 = new DigestOutputStream(fout1, digester);
        DataOutputStream out = new DataOutputStream(fos1);
        try {
            DigestOutputStream fos2 = new DigestOutputStream(fout2, digester);
            out = new DataOutputStream(new BufferedOutputStream(fos2));
            fout1.getChannel();
        } finally {
            callClose(fout1);
            callClose(fout2);
        }
    }

    void testReassignmentSetSizeOne(@Owning FilterOutputStream out) throws IOException {
        out = new DataOutputStream(out);
        out.close();
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    void callClose(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {

        }
    }
}
