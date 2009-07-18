package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class ProcessBuilder{
  public ProcessBuilder(java.util.List<java.lang.String> a1) { throw new RuntimeException("skeleton method"); }
  public ProcessBuilder(java.lang.String[] a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.ProcessBuilder command(java.util.List<java.lang.String> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.ProcessBuilder command(java.lang.String[] a1) { throw new RuntimeException("skeleton method"); }
  public java.util.List<java.lang.String> command() { throw new RuntimeException("skeleton method"); }
  public java.util.Map<java.lang.String, java.lang.String> environment() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.File directory() { throw new RuntimeException("skeleton method"); }
  public java.lang.ProcessBuilder directory(@Nullable java.io.File a1) { throw new RuntimeException("skeleton method"); }
  public boolean redirectErrorStream() { throw new RuntimeException("skeleton method"); }
  public java.lang.ProcessBuilder redirectErrorStream(boolean a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.Process start() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
