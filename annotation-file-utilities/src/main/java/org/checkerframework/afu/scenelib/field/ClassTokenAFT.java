package org.checkerframework.afu.scenelib.field;

/**
 * A {@link ClassTokenAFT} is the type of an annotation field that holds a class token (something
 * like <code>{@link String}.class</code>).
 *
 * <p>Use the singleton {@link #ctaft}.
 *
 * <p>Even if the field type was originally some parameterization <code>{@link Class}&lt;...&gt;
 * </code>, the annotation scene library represents it as a plain {@link ClassTokenAFT}.
 */
public final class ClassTokenAFT extends ScalarAFT {

  /** Create the singleton {@link #ctaft}. */
  private ClassTokenAFT() {}

  /** The singleton {@link ClassTokenAFT}. */
  public static final ClassTokenAFT ctaft = new ClassTokenAFT();

  @Override
  public boolean isValidValue(Object o) {
    return o instanceof java.lang.Class;
  }

  @Override
  public String toString() {
    return "Class";
  }

  @Override
  public void format(StringBuilder sb, Object o) {
    sb.append(((java.lang.Class<?>) o).getName());
    sb.append(".class");
  }

  @Override
  public <R, T> R accept(AFTVisitor<R, T> v, T arg) {
    return v.visitClassTokenAFT(this, arg);
  }
}
