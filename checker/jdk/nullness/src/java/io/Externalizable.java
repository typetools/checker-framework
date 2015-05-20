package java.io;

public interface Externalizable extends Serializable {
  void writeExternal(ObjectOutput a1) throws IOException;
  void readExternal(ObjectInput a1) throws IOException, ClassNotFoundException;
}
