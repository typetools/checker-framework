package java.io;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.security.AccessController;
import java.security.SecureRandom;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.attribute.FileAttribute;

import checkers.javari.quals.*;

public class File implements Serializable, Comparable<File> {
    private static final long serialVersionUID = 0L;

    static private FileSystem fs = FileSystem.getFileSystem();

    public static final char separatorChar = fs.getSeparator();
    public static final String separator = "" + separatorChar;
    public static final char pathSeparatorChar = fs.getPathSeparator();
    public static final String pathSeparator = "" + pathSeparatorChar;

    public File(String pathname) {
        throw new RuntimeException("skeleton method");
    }
    public File(String parent, String child) {
        throw new RuntimeException("skeleton method");
    }
    public File(@PolyRead File this, @PolyRead File parent, String child) {
      throw new RuntimeException("skeleton method");
    }
    public File(@ReadOnly URI uri) {
        throw new RuntimeException("skeleton method");
    }

    public String getName(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public String getParent(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public @PolyRead File getParentFile(@PolyRead File this) {
        throw new RuntimeException("skeleton method");
    }
    public String getPath(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean isAbsolute(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public String getAbsolutePath(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public @PolyRead File getAbsoluteFile(@PolyRead File this) {
        throw new RuntimeException("skeleton method");
    }
    public String getCanonicalPath(@ReadOnly File this) throws IOException {
        throw new RuntimeException("skeleton method");
    }
    public @PolyRead File getCanonicalFile(@PolyRead File this) throws IOException {
        throw new RuntimeException("skeleton method");
    }
    @Deprecated
    public @PolyRead URL toURL(@PolyRead File this) throws MalformedURLException {
        throw new RuntimeException("skeleton method");
    }
    public @PolyRead URI toURI(@PolyRead File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean canRead(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean canWrite(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean exists(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean isDirectory(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean isFile(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean isHidden(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public long lastModified(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public long length(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public boolean createNewFile() throws IOException {
        throw new RuntimeException("skeleton method");
    }
    public boolean delete() {
        throw new RuntimeException("skeleton method");
    }
    public void deleteOnExit() {
        throw new RuntimeException("skeleton method");
    }
    public String[] list(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public String[] list(@ReadOnly File this, FilenameFilter filter) {
        throw new RuntimeException("skeleton method");
    }
    public @PolyRead File [] listFiles(@PolyRead File this) {
        throw new RuntimeException("skeleton method");
    }
    public @PolyRead File [] listFiles(@PolyRead File this, FilenameFilter filter) {
        throw new RuntimeException("skeleton method");
    }
    public @PolyRead File [] listFiles(@PolyRead File this, FileFilter filter) {
        throw new RuntimeException("skeleton method");
    }
    public boolean mkdir() {
        throw new RuntimeException("skeleton method");
    }
    public boolean mkdirs() {
        throw new RuntimeException("skeleton method");
    }
    public boolean renameTo(File dest) {
        throw new RuntimeException("skeleton method");
    }
    public boolean setLastModified(long time) {
        throw new RuntimeException("skeleton method");
    }
    public boolean setReadOnly() {
        throw new RuntimeException("skeleton method");
    }
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        throw new RuntimeException("skeleton method");
    }
    public boolean setWritable(boolean writable) {
        throw new RuntimeException("skeleton method");
    }
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        throw new RuntimeException("skeleton method");
    }
    public boolean setReadable(boolean readable) {
        throw new RuntimeException("skeleton method");
    }
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        throw new RuntimeException("skeleton method");
    }
    public boolean setExecutable(boolean executable) {
        throw new RuntimeException("skeleton method");
    }
    public boolean canExecute(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public static File[] listRoots() {
        throw new RuntimeException("skeleton method");
    }
    public long getTotalSpace(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public long getFreeSpace(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public long getUsableSpace(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        throw new RuntimeException("skeleton method");
    }
    public static File createTempFile(String prefix, String suffix) throws IOException {
        throw new RuntimeException("skeleton method");
    }
//     public static File createTemporaryFile(String prefix, String suffx, @ReadOnly FileAttribute<?>... attrs) {
//         throw new RuntimeException("skeleton method");
//     }
    public int compareTo(@ReadOnly File this, @ReadOnly File pathname) {
        throw new RuntimeException("skeleton method");
    }
    public boolean equals(@ReadOnly File this, @ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }
    public int hashCode(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
    public String toString(@ReadOnly File this) {
        throw new RuntimeException("skeleton method");
    }
//     public Path toPath(@ReadOnly File this) {
//         throw new RuntimeException("skeleton method");
//     }

}
