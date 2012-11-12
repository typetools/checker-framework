package java.io;

import checkers.javari.quals.*;

public interface DataOutput {
    public void write(byte[] b) throws IOException;
    public void write(byte[] b, int len, int off) throws IOException;
    public void write(int b) throws IOException;
    public void writeBoolean(boolean v) throws IOException;
    public void writeByte(int v) throws IOException;
    public void writeBytes(String s) throws IOException;
    public void writeChar(int v) throws IOException;
    public void writeChars(String s) throws IOException;
    public void writeDouble(double v) throws IOException;
    public void writeFloat(float v) throws IOException;
    public void writeInt(int v) throws IOException;
    public void writeLong(long v) throws IOException;
    public void writeShort(int v) throws IOException;
    public void writeUTF(String s) throws IOException;
}
