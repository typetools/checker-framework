package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FileReader extends InputStreamReader {
  public FileReader(java.lang.String a1) throws java.io.FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public FileReader(java.io.File a1) throws java.io.FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public FileReader(java.io.FileDescriptor a1) { super(null); throw new RuntimeException("skeleton method"); }
}
