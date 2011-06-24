package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class File implements Serializable, Comparable<File> {
  private static final long serialVersionUID = 0;
  public final static char separatorChar;
  public final static String separator;
  public final static char pathSeparatorChar;
  public final static String pathSeparator;
  public File(String a1) { throw new RuntimeException("skeleton method"); }
  public File(@Nullable String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public File(@Nullable File a1, String a2) { throw new RuntimeException("skeleton method"); }
  public File(java.net.URI a1) { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public @Pure @Nullable String getParent() { throw new RuntimeException("skeleton method"); }
  public @Pure @Nullable File getParentFile() { throw new RuntimeException("skeleton method"); }
  public String getPath() { throw new RuntimeException("skeleton method"); }
  public boolean isAbsolute() { throw new RuntimeException("skeleton method"); }
  public String getAbsolutePath() { throw new RuntimeException("skeleton method"); }
  public File getAbsoluteFile() { throw new RuntimeException("skeleton method"); }
  public String getCanonicalPath() throws IOException { throw new RuntimeException("skeleton method"); }
  public File getCanonicalFile() throws IOException { throw new RuntimeException("skeleton method"); }
  public java.net.URL toURL() throws java.net.MalformedURLException { throw new RuntimeException("skeleton method"); }
  public java.net.URI toURI() { throw new RuntimeException("skeleton method"); }
  public boolean canRead() { throw new RuntimeException("skeleton method"); }
  public boolean canWrite() { throw new RuntimeException("skeleton method"); }
  public boolean exists() { throw new RuntimeException("skeleton method"); }
  // This @AssertNonNullIfTrue is not true, since the list methods also
  // return null in the case of an IO error (instead of throwing IOException).
  // @AssertNonNullIfTrue({"list()","list(FilenameFilter)","listFiles()","listFiles(FilenameFilter)","listFiles(FileFilter)"})
  public boolean isDirectory() { throw new RuntimeException("skeleton method"); }
  public boolean isFile() { throw new RuntimeException("skeleton method"); }
  public boolean isHidden() { throw new RuntimeException("skeleton method"); }
  public long lastModified() { throw new RuntimeException("skeleton method"); }
  public long length() { throw new RuntimeException("skeleton method"); }
  public boolean createNewFile() throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean delete() { throw new RuntimeException("skeleton method"); }
  public void deleteOnExit() { throw new RuntimeException("skeleton method"); }
  public String @Nullable [] list() { throw new RuntimeException("skeleton method"); }
  public String @Nullable [] list(@Nullable FilenameFilter a1) { throw new RuntimeException("skeleton method"); }
  public File @Nullable [] listFiles() { throw new RuntimeException("skeleton method"); }
  public File @Nullable [] listFiles(@Nullable FilenameFilter a1) { throw new RuntimeException("skeleton method"); }
  public File @Nullable [] listFiles(@Nullable FileFilter a1) { throw new RuntimeException("skeleton method"); }
  public boolean mkdir() { throw new RuntimeException("skeleton method"); }
  public boolean mkdirs() { throw new RuntimeException("skeleton method"); }
  public boolean renameTo(File a1) { throw new RuntimeException("skeleton method"); }
  public boolean setLastModified(long a1) { throw new RuntimeException("skeleton method"); }
  public boolean setReadOnly() { throw new RuntimeException("skeleton method"); }
  public boolean setWritable(boolean a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public boolean setWritable(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean setReadable(boolean a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public boolean setReadable(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean setExecutable(boolean a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public boolean setExecutable(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean canExecute() { throw new RuntimeException("skeleton method"); }
  public static File @Nullable [] listRoots() { throw new RuntimeException("skeleton method"); }
  public long getTotalSpace() { throw new RuntimeException("skeleton method"); }
  public long getFreeSpace() { throw new RuntimeException("skeleton method"); }
  public long getUsableSpace() { throw new RuntimeException("skeleton method"); }
  public static File createTempFile(String a1, @Nullable String a2, @Nullable File a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public static File createTempFile(String a1, @Nullable String a2) throws IOException { throw new RuntimeException("skeleton method"); }
  public int compareTo(File a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
