package java.io;

import checkers.javari.quals.*;

public interface DataInput {
    public boolean readBoolean();
    public byte readByte();
    public char readChar();
    public double readDouble();
    public float readFloat();
    public void readFully(byte[] b);
    public void readFully(byte[] b, int off, int len);
    public int readInt();
    public String readLine();
    public long readLong();
    public short readShort();
    public int readUnsignedByte();
    public int readUnsignedShort();
    public String readUTF();
    public int skipBytes(int n);
}
