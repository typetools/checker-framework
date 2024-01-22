package org.checkerframework.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/** Utilities for executing external processes. */
public class ExecUtil {

  public static int execute(String[] cmd, OutputStream std, OutputStream err) {

    Redirection outRedirect = new Redirection(std, BLOCK_SIZE);
    Redirection errRedirect = new Redirection(err, BLOCK_SIZE);

    try {
      Process proc = Runtime.getRuntime().exec(cmd);
      outRedirect.redirect(proc.getInputStream());
      errRedirect.redirect(proc.getErrorStream());

      IOException stdExc = outRedirect.join();
      IOException errExc = errRedirect.join();
      int exitStatus = proc.waitFor();

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

    public Redirection(OutputStream out, int bufferSize) {
      this.buffer = new char[bufferSize];
      this.out = new OutputStreamWriter(out);
    }

    public void redirect(InputStream inStream) {

      exception = null;

      this.thread =
          new Thread(
              () -> {
                try (InputStreamReader in = new InputStreamReader(inStream)) {
                  int read = 0;
                  while (read > -1) {
                    read = in.read(buffer);
                    if (read > 0) {
                      out.write(buffer, 0, read);
                    }
                  }
                  out.flush();
                } catch (IOException exc) {
                  exception = exc;
                }
              });
      thread.start();
    }

    public IOException join() throws InterruptedException {
      thread.join();
      return exception;
    }
  }
}
