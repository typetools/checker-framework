package java.io;

import checkers.nonnull.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nonnull.quals.NonNull.class)

public class FileReader extends InputStreamReader {
  public FileReader(String a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public FileReader(File a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public FileReader(FileDescriptor a1) { super(null); throw new RuntimeException("skeleton method"); }
}
