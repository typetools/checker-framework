package java.util.zip;
import checkers.javari.quals.*;

public class ZipEntry implements ZipConstants, Cloneable {
    public static final int STORED = 0;
    public static final int DEFLATED = 8;

    public ZipEntry(String name) {
        throw new RuntimeException("skeleton method");
    }

    public ZipEntry(@ReadOnly ZipEntry e) {
        throw new RuntimeException("skeleton method");
    }

    public String getName() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void setTime(long time) {
        throw new RuntimeException("skeleton method");
    }

    public long getTime() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void setSize(long size) {
        throw new RuntimeException("skeleton method");
    }

    public long getSize() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public long getCompressedSize() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void setCompressedSize(long csize) {
        throw new RuntimeException("skeleton method");
    }

    public void setCrc(long crc) {
        throw new RuntimeException("skeleton method");
    }

    public long getCrc() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void setMethod(int method) {
        throw new RuntimeException("skeleton method");
    }

    public int getMethod() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void setExtra(byte[] extra) {
        throw new RuntimeException("skeleton method");
    }

    public byte[] getExtra() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void setComment(String comment) {
        throw new RuntimeException("skeleton method");
    }

    public String getComment() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public boolean isDirectory() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String toString() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Object clone() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }
}
