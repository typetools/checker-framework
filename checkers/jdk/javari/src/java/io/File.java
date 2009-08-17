package java.io;

import java.net.URI;
import java.net.URL;

import checkers.javari.quals.*;

public class File implements Serializable, Comparable<File> {
    public static String pathSeparator;
    public static char pathSeparatorChar;
    public static String separator;
    public static char separatorChar;

    public File(@PolyRead File parent, String child) @PolyRead { throw new RuntimeException("skeleton method"); }
    public File(String pathname) { throw new RuntimeException("skeleton method"); }
    public File(String parent, String child) { throw new RuntimeException("skeleton method"); }
    public File(@ReadOnly URI uri) { throw new RuntimeException("skeleton method"); }

    public boolean canExecute() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean canRead() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean canWrite() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int compareTo(@ReadOnly File pathname) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean createNewFile() { throw new RuntimeException("skeleton method"); }
    public static File createTempFile(String prefix, String suffix) { throw new RuntimeException("skeleton method"); }
    public static File createTempFile(String prefix, String suffix, File directory) { throw new RuntimeException("skeleton method"); }
    public boolean delete() { throw new RuntimeException("skeleton method"); }
    public void deleteOnExit() { throw new RuntimeException("skeleton method"); }
    public boolean equals(@ReadOnly Object obj) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public @PolyRead File getAbsoluteFile() @PolyRead { throw new RuntimeException("skeleton method"); }
    public String getAbsolutePath() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public @PolyRead File getCanonicalFile() @PolyRead { throw new RuntimeException("skeleton method"); }
    public long getFreeSpace() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public String getName() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public String getParent() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public @PolyRead File getParentFile() @PolyRead() { throw new RuntimeException("skeleton method"); }
    public String getPath() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public long getTotalSpace() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public long getUsableSpace() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean isAbsolute() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean isDirectory() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean isFile() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public boolean isHidden() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public long lastModified() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public long length() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public String[] list() @ReadOnly { throw new RuntimeException("skeleton method"); }
    public String[] list(FilenameFilter filter) @ReadOnly { throw new RuntimeException("skeleton method"); }
    public @PolyRead File [] listFiles() @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead File [] listFiles(FileFilter filter) @PolyRead { throw new RuntimeException("skeleton method"); }
    public @PolyRead File [] listFiles(FilenameFilter filter) @PolyRead { throw new RuntimeException("skeleton method"); }
    public static File[] listRoots() { throw new RuntimeException("skeleton method"); }
    public boolean mkdir() { throw new RuntimeException("skeleton method"); }
    public boolean mkdirs() { throw new RuntimeException("skeleton method"); }
    public boolean renameTo(File dest) { throw new RuntimeException("skeleton method"); }
    public boolean setExecutable(boolean executable) { throw new RuntimeException("skeleton method"); }
    public boolean setExecutable(boolean executable, boolean ownerOnly) { throw new RuntimeException("skeleton method"); }
    public boolean setLastModified(long time) { throw new RuntimeException("skeleton method"); }
    public boolean setReadable(boolean readable) { throw new RuntimeException("skeleton method"); }
    public boolean setReadable(boolean readable, boolean ownerOnly) { throw new RuntimeException("skeleton method"); }
    public boolean setReadOnly() { throw new RuntimeException("skeleton method"); }
    public boolean setWriteable(boolean writeable) { throw new RuntimeException("skeleton method"); }
    public boolean setWriteable(boolean writeable, boolean ownerOnly) { throw new RuntimeException("skeleton method"); }
    public String toString() @ReadOnly  { throw new RuntimeException("skeleton method"); }
    public @PolyRead URI toURI() @PolyRead { throw new RuntimeException("skeleton method"); }
    @Deprecated public @PolyRead URL toURL() @PolyRead { throw new RuntimeException("skeleton method"); }
}
