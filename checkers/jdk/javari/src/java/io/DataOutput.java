package java.io;

import checkers.javari.quals.*;

public interface DataOutput {
    public void write(byte[] b);
    public void write(byte[] b, int len, int off);
    public void write(byte b);
    public void writeBoolean(boolean v);
    public void writeByte(int v);
    public void writeBytes(String s);
    public void writeChar(int v);
    public void writeChars(String s);
    public void writeDouble(double v);
    public void writeFloat(float v);
    public void writeInt(int v);
    public void writeLong(long v);
    public void writeShort(int v);
    public void writeUTF(String s);
}
