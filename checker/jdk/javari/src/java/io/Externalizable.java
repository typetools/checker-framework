package java.io;

import org.checkerframework.checker.javari.qual.*;

public interface Externalizable extends Serializable {
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;
    public void writeExternal(ObjectOutput out) throws IOException;
}
