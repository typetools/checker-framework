// A test case for a false positive in hfds.

import java.io.InputStream;

class StreamBool {
    InputStream stream;

    boolean isActive() {
        return stream != null;
    }
}
