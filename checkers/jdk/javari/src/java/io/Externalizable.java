package java.io;

import checkers.javari.quals.*;

public interface Externalizable {
    public void readExternal(ObjectInput in);
    public void writeExternal(ObjectOutput out);
}
