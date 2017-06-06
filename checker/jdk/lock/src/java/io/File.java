package java.io;


import org.checkerframework.checker.lock.qual.*;



public class File implements Serializable, Comparable<File> {
  private static final long serialVersionUID = 0;
  public final static char separatorChar = ':';
  public final static String separator = ":";
  public final static char pathSeparatorChar = '/';
  public final static String pathSeparator = "/";
  public File(String a1) { throw new RuntimeException("skeleton method"); }
  public File(String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public File(File a1, String a2) { throw new RuntimeException("skeleton method"); }
  public File(java.net.URI a1) { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
   public String getParent(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
   public File getParentFile(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
  public String getPath() { throw new RuntimeException("skeleton method"); }
   public boolean isAbsolute(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
  public String getAbsolutePath() { throw new RuntimeException("skeleton method"); }
  public File getAbsoluteFile() { throw new RuntimeException("skeleton method"); }
  public String getCanonicalPath() throws IOException { throw new RuntimeException("skeleton method"); }
  public File getCanonicalFile() throws IOException { throw new RuntimeException("skeleton method"); }
  public java.net.URL toURL() throws java.net.MalformedURLException { throw new RuntimeException("skeleton method"); }
  public java.net.URI toURI() { throw new RuntimeException("skeleton method"); }
  public boolean canRead() { throw new RuntimeException("skeleton method"); }
  public boolean canWrite() { throw new RuntimeException("skeleton method"); }
  public boolean exists() { throw new RuntimeException("skeleton method"); }
   public boolean isDirectory(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
   public boolean isFile(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
   public boolean isHidden(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
  public long lastModified() { throw new RuntimeException("skeleton method"); }
  public long length() { throw new RuntimeException("skeleton method"); }
  public boolean createNewFile() throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean delete() { throw new RuntimeException("skeleton method"); }
  public void deleteOnExit() { throw new RuntimeException("skeleton method"); }
  public String [] list() { throw new RuntimeException("skeleton method"); }
  public String [] list(FilenameFilter a1) { throw new RuntimeException("skeleton method"); }
  public File [] listFiles() { throw new RuntimeException("skeleton method"); }
  public File [] listFiles(FilenameFilter a1) { throw new RuntimeException("skeleton method"); }
  public File [] listFiles(FileFilter a1) { throw new RuntimeException("skeleton method"); }
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
  public static File [] listRoots() { throw new RuntimeException("skeleton method"); }
  public long getTotalSpace() { throw new RuntimeException("skeleton method"); }
  public long getFreeSpace() { throw new RuntimeException("skeleton method"); }
  public long getUsableSpace() { throw new RuntimeException("skeleton method"); }
  public static File createTempFile(String a1, String a2, File a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public static File createTempFile(String a1, String a2) throws IOException { throw new RuntimeException("skeleton method"); }
   public int compareTo(@GuardSatisfied File this,@GuardSatisfied File a1) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied File this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied File this) { throw new RuntimeException("skeleton method"); }
}
