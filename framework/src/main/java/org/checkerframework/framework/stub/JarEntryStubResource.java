package org.checkerframework.framework.stub;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** {@link JarEntry}-based implementation of {@link StubResource}. */
public class JarEntryStubResource implements StubResource {
    private final JarFile file;
    private final JarEntry entry;

    /**
     * Constructs a {@code StubResource} for the specified entry in the specified JAR file.
     *
     * @param file the JAR file
     * @param entry the JAR entry
     */
    public JarEntryStubResource(JarFile file, JarEntry entry) {
        this.file = file;
        this.entry = entry;
    }

    @Override
    public String getDescription() {
        return file.getName() + "!" + entry.getName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return file.getInputStream(entry);
    }
}
