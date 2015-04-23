package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public abstract interface Externalizable extends Serializable {
  public abstract void writeExternal(ObjectOutput a1) throws IOException;
  public abstract void readExternal(ObjectInput a1) throws IOException, ClassNotFoundException;
}
