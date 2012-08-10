package java.io;

import checkers.nonnull.quals.Nullable;


public abstract interface Externalizable extends Serializable {
  public abstract void writeExternal(ObjectOutput a1) throws IOException;
  public abstract void readExternal(ObjectInput a1) throws IOException, ClassNotFoundException;
}
