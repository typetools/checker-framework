package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class Process{
  public Process() { throw new RuntimeException("skeleton method"); }
  public abstract java.io.OutputStream getOutputStream();
  public abstract java.io.InputStream getInputStream();
  public abstract java.io.InputStream getErrorStream();
  public abstract int waitFor() throws InterruptedException;
  public abstract int exitValue();
  public abstract void destroy();
}
