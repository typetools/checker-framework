package org.checkerframework.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class ExecUtil {

    public static int execute(final String[] cmd, final OutputStream std, final OutputStream err) {

        final Redirection outRedirect = new Redirection(std, BLOCK_SIZE);
        final Redirection errRedirect = new Redirection(err, BLOCK_SIZE);

        try {
            final Process proc = Runtime.getRuntime().exec(cmd);
            outRedirect.redirect(proc.getInputStream());
            errRedirect.redirect(proc.getErrorStream());

            final IOException stdExc = outRedirect.join();
            final IOException errExc = errRedirect.join();
            final int exitStatus = proc.waitFor();

            if (stdExc != null) {
                throw stdExc;
            }

            if (errExc != null) {
                throw errExc;
            }

            return exitStatus;

        } catch (InterruptedException e) {
            throw new RuntimeException("Exception executing command: " + String.join(" ", cmd), e);
        } catch (IOException e) {
            throw new RuntimeException("Exception executing command: " + String.join(" ", cmd), e);
        }
    }

    public static final int BLOCK_SIZE = 1024;

    public static class Redirection {
        private final char[] buffer;
        private final OutputStreamWriter out;

        private Thread thread;
        private IOException exception;

        public Redirection(final OutputStream out, final int bufferSize) {
            this.buffer = new char[bufferSize];
            this.out = new OutputStreamWriter(out);
        }

        public void redirect(final InputStream inStream) {

            exception = null;

            this.thread =
                    new Thread(
                            () -> {
                                final InputStreamReader in = new InputStreamReader(inStream);
                                try {

                                    int read = 0;
                                    while (read > -1) {
                                        read = in.read(buffer);
                                        if (read > 0) {
                                            out.write(buffer, 0, read);
                                        }
                                        out.flush();
                                    }

                                } catch (IOException exc) {
                                    exception = exc;
                                } finally {
                                    quietlyClose(in);
                                }
                            });
            thread.start();
        }

        public IOException join() throws InterruptedException {
            thread.join();
            return exception;
        }
    }

    /**
     * Close the given writer, ignoring exceptions.
     *
     * @param writer the writer to close
     */
    @SuppressWarnings("EmptyCatch") // the purpose of this method is to ignore exceptions
    public static void quietlyClose(final Writer writer) {
        try {
            writer.close();
        } catch (IOException ioExc) {
        }
    }

    /**
     * Close the given reader, ignoring exceptions.
     *
     * @param reader the reader to close
     */
    @SuppressWarnings("EmptyCatch") // the purpose of this method is to ignore exceptions
    public static void quietlyClose(final Reader reader) {
        try {
            reader.close();
        } catch (IOException ioExc) {
        }
    }
}
