import java.io.Serializable;

@SuppressWarnings("deprecation") // newInstance is deprecated.
public class SignednessCast {
  static class Instruction {}

  public Instruction createCast(String name) {
    Instruction i = null;
    try {
      i = (Instruction) java.lang.Class.forName(name).newInstance();
    } catch (final Exception e) {
      throw new IllegalArgumentException("Could not find instruction: " + name, e);
    }
    return i;
  }

  static class SerializableInstruction implements Serializable {}

  SerializableInstruction other(String name) {
    SerializableInstruction i = null;
    try {
      i = (SerializableInstruction) java.lang.Class.forName(name).newInstance();
    } catch (final Exception e) {
      throw new IllegalArgumentException("Could not find instruction: " + name, e);
    }
    return i;
  }

  Serializable maybeNumber(String name) {
    Serializable i = null;
    try {
      i = (Serializable) java.lang.Class.forName(name).newInstance();
    } catch (final Exception e) {
      throw new IllegalArgumentException("Could not find instruction: " + name, e);
    }
    // :: error: (return)
    return i;
  }
}
