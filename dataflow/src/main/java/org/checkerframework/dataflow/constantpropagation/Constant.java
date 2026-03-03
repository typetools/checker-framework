package org.checkerframework.dataflow.constantpropagation;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.javacutil.BugInCF;

public class Constant implements AbstractValue<Constant> {

  /** What kind of abstract value is this? */
  protected final Type type;

  /** The value of this abstract value (or null). */
  protected @Nullable Integer value;

  public enum Type {
    CONSTANT,
    TOP,
    BOTTOM,
  }

  /** Create a constant for {@code type}. */
  public Constant(Type type) {
    assert type != Type.CONSTANT;
    this.type = type;
  }

  /** Create a constant for {@code value}. */
  public Constant(Integer value) {
    this.type = Type.CONSTANT;
    this.value = value;
  }

  /**
   * Returns true if the constant is TOP.
   *
   * @return true if the constant is TOP
   */
  public boolean isTop() {
    return type == Type.TOP;
  }

  /**
   * Returns true if the constant is BOTTOM.
   *
   * @return true if the constant is BOTTOM
   */
  public boolean isBottom() {
    return type == Type.BOTTOM;
  }

  /**
   * Returns true if the constant is CONSTANT.
   *
   * @return true if the constant is CONSTANT
   */
  @EnsuresNonNullIf(result = true, expression = "value")
  public boolean isConstant() {
    return type == Type.CONSTANT && value != null;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  public Integer getValue() {
    assert isConstant() : "@AssumeAssertion(nullness): inspection";
    return value;
  }

  public Constant copy() {
    if (isConstant()) {
      return new Constant(value);
    }
    return new Constant(type);
  }

  @Override
  public Constant leastUpperBound(Constant other) {
    if (other.isBottom()) {
      return this.copy();
    }
    if (this.isBottom()) {
      return other.copy();
    }
    if (other.isTop() || this.isTop()) {
      return new Constant(Type.TOP);
    }
    if (other.getValue().equals(getValue())) {
      return this.copy();
    }
    return new Constant(Type.TOP);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof Constant)) {
      return false;
    }
    Constant other = (Constant) obj;
    return type == other.type && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, value);
  }

  @Override
  public String toString() {
    switch (type) {
      case TOP:
        return "T";
      case BOTTOM:
        return "-";
      case CONSTANT:
        assert isConstant() : "@AssumeAssertion(nullness)";
        return value.toString();
      default:
        throw new BugInCF("Unexpected type: " + type);
    }
  }
}
