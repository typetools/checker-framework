// Test case based on an MCA situation in Zookeeper.
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

class MustCallAliasLayeredStreams {
    InputStream cache;

    public InputStream createInputStream(String filename) throws FileNotFoundException {
        if (cache == null) {
            // The real version of this uses a mix of JDK and custom streams, so it makes more
            // sense...
            // TODO we shouldn't report a warning here and the code is okay because the cache is
            // non-owning, and the caller of createInputStream is the owner of all of these streams.
            // :: error: required.method.not.called
            cache =
                    new DataInputStream(
                            new BufferedInputStream(new FileInputStream(new File(filename))));
        }
        return cache;
    }
}
