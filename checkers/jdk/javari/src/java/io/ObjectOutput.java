package java.io;

import checkers.javari.quals.*;

public interface ObjectOutput extends DataOutput {
    public void close() throws IOException;
    public void flush() throws IOException;
    public void write(byte @ReadOnly [] b) throws IOException;
    public void write(byte @ReadOnly [] b, int off, int len) throws IOException;
    public void write(int b) throws IOException;
    public void writeObject(@ReadOnly Object obj) throws IOException;
}
