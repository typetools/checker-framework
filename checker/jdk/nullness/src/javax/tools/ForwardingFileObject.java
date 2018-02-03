/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package javax.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Forwards calls to a given file object.  Subclasses of this class
 * might override some of these methods and might also provide
 * additional fields and methods.
 *
 * @param <F> the kind of file object forwarded to by this object
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public class ForwardingFileObject<F extends FileObject> implements FileObject {

    /**
     * The file object which all methods are delegated to.
     */
    protected final F fileObject;

    /**
     * Creates a new instance of ForwardingFileObject.
     * @param fileObject delegate to this file object
     */
    protected ForwardingFileObject(F fileObject) {
        fileObject.getClass(); // null check
        this.fileObject = fileObject;
    }

    public URI toUri() {
        return fileObject.toUri();
    }

    public String getName() {
        return fileObject.getName();
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public InputStream openInputStream() throws IOException {
        return fileObject.openInputStream();
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public OutputStream openOutputStream() throws IOException {
        return fileObject.openOutputStream();
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return fileObject.openReader(ignoreEncodingErrors);
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @SuppressWarnings("nullness") 
    public @Nullable CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return fileObject.getCharContent(ignoreEncodingErrors);
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public Writer openWriter() throws IOException {
        return fileObject.openWriter();
    }

    public long getLastModified() {
        return fileObject.getLastModified();
    }

    public boolean delete() {
        return fileObject.delete();
    }
}
