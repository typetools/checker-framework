package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class ProcessBuilder{
  public ProcessBuilder(java.util.List<String> a1) { throw new RuntimeException("skeleton method"); }
  public ProcessBuilder(String... a1) { throw new RuntimeException("skeleton method"); }
  public ProcessBuilder command(java.util.List<String> a1) { throw new RuntimeException("skeleton method"); }
  public ProcessBuilder command(String... a1) { throw new RuntimeException("skeleton method"); }
  public java.util.List<String> command() { throw new RuntimeException("skeleton method"); }
  public java.util.Map<String, String> environment() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.File directory() { throw new RuntimeException("skeleton method"); }
  public ProcessBuilder directory(@Nullable java.io.File a1) { throw new RuntimeException("skeleton method"); }
  public boolean redirectErrorStream() { throw new RuntimeException("skeleton method"); }
  public ProcessBuilder redirectErrorStream(boolean a1) { throw new RuntimeException("skeleton method"); }
  public Process start() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
