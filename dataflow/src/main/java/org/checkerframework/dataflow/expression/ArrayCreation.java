package org.checkerframework.dataflow.expression;

import java.util.List;
import java.util.Objects;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/** JavaExpression for array creations. {@code new String[]()}. */
public class ArrayCreation extends JavaExpression {

    /**
     * List of dimensions expressions. A {code null} element means that there is no dimension
     * expression for the given array level.
     */
    protected final List<@Nullable JavaExpression> dimensions;
    /** List of initializers. */
    protected final List<JavaExpression> initializers;

    /**
     * Creates an ArrayCreation object.
     *
     * @param type array type
     * @param dimensions list of dimension expressions; a {code null} element means that there is no
     *     dimension expression for the given array level.
     * @param initializers list of initializer expressions
     */
    public ArrayCreation(
            TypeMirror type,
            List<@Nullable JavaExpression> dimensions,
            List<JavaExpression> initializers) {
        super(type);
        assert type.getKind() == TypeKind.ARRAY;
        this.dimensions = dimensions;
        this.initializers = initializers;
    }

    /**
     * Returns a list representing the dimensions of this array creation. A {code null} element
     * means that there is no dimension expression for the given array level.
     *
     * @return a list representing the dimensions of this array creation
     */
    public List<@Nullable JavaExpression> getDimensions() {
        return dimensions;
    }

    public List<JavaExpression> getInitializers() {
        return initializers;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        for (JavaExpression n : dimensions) {
            if (n != null && n.getClass() == clazz) {
                return true;
            }
        }
        for (JavaExpression n : initializers) {
            if (n.getClass() == clazz) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        return false;
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensions, initializers, getType().toString());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ArrayCreation)) {
            return false;
        }
        ArrayCreation other = (ArrayCreation) obj;
        return this.dimensions.equals(other.getDimensions())
                && this.initializers.equals(other.getInitializers())
                // It might be better to use Types.isSameType(getType(), other.getType()), but I
                // don't have a Types object.
                && getType().toString().equals(other.getType().toString());
    }

    @Override
    public boolean syntacticEquals(JavaExpression je) {
        if (!(je instanceof ArrayCreation)) {
            return false;
        }
        ArrayCreation other = (ArrayCreation) je;
        return JavaExpression.syntacticEqualsList(this.dimensions, other.dimensions)
                && JavaExpression.syntacticEqualsList(this.initializers, other.initializers)
                && getType().toString().equals(other.getType().toString());
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return syntacticEquals(other)
                || JavaExpression.listContainsSyntacticEqualJavaExpression(dimensions, other)
                || JavaExpression.listContainsSyntacticEqualJavaExpression(initializers, other);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (dimensions.isEmpty()) {
            sb.append("new " + type);
        } else {
            sb.append("new " + TypesUtils.getInnermostComponentType((ArrayType) type));
            for (JavaExpression dim : dimensions) {
                sb.append("[");
                sb.append(dim == null ? "" : dim);
                sb.append("]");
            }
        }
        if (!initializers.isEmpty()) {
            sb.append(" {");
            sb.append(StringsPlume.join(", ", initializers));
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public String toStringDebug() {
        return "\""
                + super.toStringDebug()
                + "\""
                + " type="
                + type
                + " dimensions="
                + dimensions
                + " initializers="
                + initializers;
    }

    @Override
    @SuppressWarnings("interning:not.interned") // test whether method returns its argument
    public ArrayCreation atMethodSignature(List<JavaExpression> parameters) {
        List<@Nullable JavaExpression> newDimensions =
                JavaExpression.listAtMethodSignature(dimensions, parameters);
        List<JavaExpression> newInitializers =
                JavaExpression.listAtMethodSignature(initializers, parameters);
        if (dimensions == newDimensions && initializers == newInitializers) {
            return this;
        } else {
            return new ArrayCreation(type, newDimensions, newInitializers);
        }
    }
}
