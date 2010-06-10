package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FileReader extends InputStreamReader {
  public FileReader(String a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public FileReader(File a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public FileReader(FileDescriptor a1) { super(null); throw new RuntimeException("skeleton method"); }
}
