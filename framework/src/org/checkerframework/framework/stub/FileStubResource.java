package org.checkerframework.framework.stub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** {@link File}-based implementation of {@link StubResource}. */
public class FileStubResource implements StubResource {
    private final File file;

    /**
     * Constructs a {@code StubResource} for the specified stub file.
     *
     * @param file the stub file
     */
    public FileStubResource(File file) {
        this.file = file;
    }

    @Override
    public String getDescription() {
        return file.getAbsolutePath();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }
}
