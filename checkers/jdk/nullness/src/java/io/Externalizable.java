package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface Externalizable extends Serializable {
  public abstract void writeExternal(java.io.ObjectOutput a1) throws java.io.IOException;
  public abstract void readExternal(java.io.ObjectInput a1) throws java.io.IOException, java.lang.ClassNotFoundException;
}
