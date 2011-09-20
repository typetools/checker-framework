package java.io;

import checkers.javari.quals.*;

public interface Externalizable extends Serializable {
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;
    public void writeExternal(ObjectOutput out) throws IOException;
}
