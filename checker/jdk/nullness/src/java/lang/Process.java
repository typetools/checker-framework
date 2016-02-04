package java.lang;

public abstract class Process{
  public Process() { throw new RuntimeException("skeleton method"); }
  // These three methods return @NonNull values despite being documented as
  // possibly returning a "null stream".  A "null stream" is a non-null
  // Stream with particular behavior, not a @Nullable Stream reference.
  public abstract java.io.OutputStream getOutputStream();
  public abstract java.io.InputStream getInputStream();
  public abstract java.io.InputStream getErrorStream();
  public abstract int waitFor() throws InterruptedException;
  public abstract int exitValue();
  public abstract void destroy();
}
