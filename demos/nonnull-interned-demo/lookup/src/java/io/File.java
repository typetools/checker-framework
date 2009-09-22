package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class File implements java.io.Serializable, java.lang.Comparable<java.io.File> {
  private static final long serialVersionUID = 0;
  public final static char separatorChar;
  public final static java.lang.String separator;
  public final static char pathSeparatorChar;
  public final static java.lang.String pathSeparator;
  public File(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public File(@Nullable java.lang.String a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public File(@Nullable java.io.File a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public File(java.net.URI a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getParent() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.File getParentFile() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getPath() { throw new RuntimeException("skeleton method"); }
  public boolean isAbsolute() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getAbsolutePath() { throw new RuntimeException("skeleton method"); }
  public java.io.File getAbsoluteFile() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getCanonicalPath() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.io.File getCanonicalFile() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.net.URL toURL() throws java.net.MalformedURLException { throw new RuntimeException("skeleton method"); }
  public java.net.URI toURI() { throw new RuntimeException("skeleton method"); }
  public boolean canRead() { throw new RuntimeException("skeleton method"); }
  public boolean canWrite() { throw new RuntimeException("skeleton method"); }
  public boolean exists() { throw new RuntimeException("skeleton method"); }
  public boolean isDirectory() { throw new RuntimeException("skeleton method"); }
  public boolean isFile() { throw new RuntimeException("skeleton method"); }
  public boolean isHidden() { throw new RuntimeException("skeleton method"); }
  public long lastModified() { throw new RuntimeException("skeleton method"); }
  public long length() { throw new RuntimeException("skeleton method"); }
  public boolean createNewFile() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public boolean delete() { throw new RuntimeException("skeleton method"); }
  public void deleteOnExit() { throw new RuntimeException("skeleton method"); }
  public java.lang.String @Nullable [] list() { throw new RuntimeException("skeleton method"); }
  public java.lang.String @Nullable [] list(@Nullable java.io.FilenameFilter a1) { throw new RuntimeException("skeleton method"); }
  public java.io.File @Nullable [] listFiles() { throw new RuntimeException("skeleton method"); }
  public java.io.File @Nullable [] listFiles(@Nullable java.io.FilenameFilter a1) { throw new RuntimeException("skeleton method"); }
  public java.io.File @Nullable [] listFiles(@Nullable java.io.FileFilter a1) { throw new RuntimeException("skeleton method"); }
  public boolean mkdir() { throw new RuntimeException("skeleton method"); }
  public boolean mkdirs() { throw new RuntimeException("skeleton method"); }
  public boolean renameTo(java.io.File a1) { throw new RuntimeException("skeleton method"); }
  public boolean setLastModified(long a1) { throw new RuntimeException("skeleton method"); }
  public boolean setReadOnly() { throw new RuntimeException("skeleton method"); }
  public boolean setWritable(boolean a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public boolean setWritable(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean setReadable(boolean a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public boolean setReadable(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean setExecutable(boolean a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public boolean setExecutable(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean canExecute() { throw new RuntimeException("skeleton method"); }
  public static java.io.File @Nullable [] listRoots() { throw new RuntimeException("skeleton method"); }
  public long getTotalSpace() { throw new RuntimeException("skeleton method"); }
  public long getFreeSpace() { throw new RuntimeException("skeleton method"); }
  public long getUsableSpace() { throw new RuntimeException("skeleton method"); }
  public static java.io.File createTempFile(java.lang.String a1, @Nullable java.lang.String a2, @Nullable java.io.File a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public static java.io.File createTempFile(java.lang.String a1, @Nullable java.lang.String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int compareTo(java.io.File a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
