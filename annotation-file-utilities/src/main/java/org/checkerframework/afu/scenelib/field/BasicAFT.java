package org.checkerframework.afu.scenelib.field;

import com.google.common.escape.CharEscaperBuilder;
import com.google.common.escape.Escaper;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@code BasicAFT} represents a primitive or {@link String} annotation field type. Get one using
 * {@link #forType(Class)}.
 */
// should be an enum except they can't be generic and can't extend a class
public final class BasicAFT extends ScalarAFT {
  static final Escaper charEscaper =
      new CharEscaperBuilder()
          .addEscape('\b', "\\b")
          .addEscape('\f', "\\f")
          .addEscape('\n', "\\n")
          .addEscape('\r', "\\r")
          .addEscape('\t', "\\t")
          .addEscape('\"', "\\\"")
          .addEscape('\\', "\\\\")
          .addEscape('\'', "\\'")
          .toEscaper();

  /** The Java type backing this annotation field type. */
  public final Class<?> type;

  private BasicAFT(Class<?> type) {
    this.type = type;
  }

  /**
   * Returns the {@code BasicAFT} for {@code type}, which should be primitive (e.g., int.class) or
   * String. Returns null if {@code type} is not appropriate for a basic annotation field type.
   */
  public static BasicAFT forType(Class<?> type) {
    return bafts.get(type);
  }

  /** Maps from {@link #type} to {@code BasicAFT}. Contains every BasicAFT. */
  // Disgusting reason for being public; need to fix.
  public static final Map<Class<?>, BasicAFT> bafts;

  static {
    Map<Class<?>, BasicAFT> tempBafts = new HashMap<>(9);
    tempBafts.put(byte.class, new BasicAFT(byte.class));
    tempBafts.put(short.class, new BasicAFT(short.class));
    tempBafts.put(int.class, new BasicAFT(int.class));
    tempBafts.put(long.class, new BasicAFT(long.class));
    tempBafts.put(float.class, new BasicAFT(float.class));
    tempBafts.put(double.class, new BasicAFT(double.class));
    tempBafts.put(char.class, new BasicAFT(char.class));
    tempBafts.put(boolean.class, new BasicAFT(boolean.class));
    tempBafts.put(String.class, new BasicAFT(String.class));
    // bafts = Collections2.<Class<?>, BasicAFT>unmodifiableKeyedSet(tempBafts);
    // bafts = bafts2;
    bafts = tempBafts;
  }

  @Override
  public boolean isValidValue(Object o) {
    return ((type == byte.class && o instanceof Byte)
        || (type == short.class && o instanceof Short)
        || (type == int.class && o instanceof Integer)
        || (type == long.class && o instanceof Long)
        || (type == float.class && o instanceof Float)
        || (type == double.class && o instanceof Double)
        || (type == char.class && o instanceof Character)
        || (type == boolean.class && o instanceof Boolean)
        || (type == String.class && o instanceof String));
  }

  @Override
  public String toString() {
    if (type == String.class) {
      return "String";
    } else {
      return type.getName();
    }
  }

  @Override
  public void format(StringBuilder sb, Object o) {
    if (type == String.class) {
      sb.append("\"");
      sb.append(charEscaper.escape((String) o));
      sb.append("\"");
    } else if (type == long.class) {
      sb.append(o.toString());
      sb.append("L");
    } else if (type == double.class && Double.isNaN((double) o)) {
      // Don't use "Double.NaN" because it is not parseable if the code imports a `Double` class
      // other than `java.lang.Double`.
      sb.append("0.0/0.0");
    } else if (type == float.class && Float.isNaN((float) o)) {
      // Don't use "Float.NaN" because it is not parseable if the code imports a `Float` class
      // other than `java.lang.Float`.
      sb.append("0.0f/0.0f");
    } else {
      sb.append(o.toString());
    }
  }

  @Override
  public <R, T> R accept(AFTVisitor<R, T> v, T arg) {
    return v.visitBasicAFT(this, arg);
  }
}
