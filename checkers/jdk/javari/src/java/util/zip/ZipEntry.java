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

    public String getName(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public void setTime(long time) {
        throw new RuntimeException("skeleton method");
    }

    public long getTime(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public void setSize(long size) {
        throw new RuntimeException("skeleton method");
    }

    public long getSize(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public long getCompressedSize(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public void setCompressedSize(long csize) {
        throw new RuntimeException("skeleton method");
    }

    public void setCrc(long crc) {
        throw new RuntimeException("skeleton method");
    }

    public long getCrc(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public void setMethod(int method) {
        throw new RuntimeException("skeleton method");
    }

    public int getMethod(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public void setExtra(byte[] extra) {
        throw new RuntimeException("skeleton method");
    }

    public byte[] getExtra(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public void setComment(String comment) {
        throw new RuntimeException("skeleton method");
    }

    public String getComment(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public boolean isDirectory(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public String toString(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }

    public Object clone(@ReadOnly ZipEntry this) {
        throw new RuntimeException("skeleton method");
    }
}
