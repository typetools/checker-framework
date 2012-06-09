package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface Externalizable extends Serializable {
  public abstract void writeExternal(ObjectOutput a1) throws IOException;
  public abstract void readExternal(ObjectInput a1) throws IOException, ClassNotFoundException;
}
