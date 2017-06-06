package org.checkerframework.framework.stub;

import java.io.IOException;
import java.io.InputStream;

/** Contract for sources of stub data. */
public interface StubResource {
    /** Returns a user-friendly description of the resource (e.g. a filesystem path). */
    String getDescription();

    /** Returns a stream for reading the contents of the resource. */
    InputStream getInputStream() throws IOException;
}
