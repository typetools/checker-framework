package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FileWriter extends OutputStreamWriter {
  public FileWriter(String a1) throws IOException { super(null); throw new RuntimeException("skeleton method"); }
  public FileWriter(String a1, boolean a2) throws IOException { super(null); throw new RuntimeException("skeleton method"); }
  public FileWriter(File a1) throws IOException { super(null); throw new RuntimeException("skeleton method"); }
  public FileWriter(File a1, boolean a2) throws IOException { super(null); throw new RuntimeException("skeleton method"); }
  public FileWriter(FileDescriptor a1) { super(null); throw new RuntimeException("skeleton method"); }
}
