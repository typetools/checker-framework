package org.checkerframework.dataflow.constantpropagation;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;

public class Constant implements AbstractValue<Constant> {

    /** What kind of abstract value is this? */
    protected Type type;

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

    /** @return whether or not the constant is TOP. */
    public boolean isTop() {
        return type == Type.TOP;
    }

    /** @return whether or not the constant is BOTTOM. */
    public boolean isBottom() {
        return type == Type.BOTTOM;
    }
    /** @return whether or not the constant is CONSTANT. */
    public boolean isConstant() {
        return type == Type.CONSTANT;
    }

    public Integer getValue() {
        assert isConstant();
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
    public boolean equals(Object obj) {
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
                return value.toString();
        }
        assert false;
        return "???";
    }
}
