package org.checkerframework.dataflow.expression;

import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.javacutil.AnnotationProvider;

/** An array access. */
public class ArrayAccess extends JavaExpression {

    /** The array being accessed. */
    protected final JavaExpression array;
    /** The index; an expression of type int. */
    protected final JavaExpression index;

    /**
     * Create a new ArrayAccess.
     *
     * @param type the type of the array access
     * @param array the array being accessed
     * @param index the index; an expression of type int
     */
    public ArrayAccess(TypeMirror type, JavaExpression array, JavaExpression index) {
        super(type);
        this.array = array;
        this.index = index;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        if (getClass() == clazz) {
            return true;
        }
        if (array.containsOfClass(clazz)) {
            return true;
        }
        return index.containsOfClass(clazz);
    }

    @Override
    public boolean isDeterministic(AnnotationProvider provider) {
        return array.isDeterministic(provider) && index.isDeterministic(provider);
    }

    /**
     * Returns the array being accessed.
     *
     * @return the array being accessed
     */
    public JavaExpression getArray() {
        return array;
    }

    public JavaExpression getIndex() {
        return index;
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
    public boolean syntacticEquals(JavaExpression je) {
        if (!(je instanceof ArrayAccess)) {
            return false;
        }
        ArrayAccess other = (ArrayAccess) je;
        return array.syntacticEquals(other.array) && index.syntacticEquals(other.index);
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return syntacticEquals(other)
                || array.containsSyntacticEqualJavaExpression(other)
                || index.containsSyntacticEqualJavaExpression(other);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        if (array.containsModifiableAliasOf(store, other)) {
            return true;
        }
        return index.containsModifiableAliasOf(store, other);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ArrayAccess)) {
            return false;
        }
        ArrayAccess other = (ArrayAccess) obj;
        return array.equals(other.array) && index.equals(other.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(array, index);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(array.toString());
        result.append("[");
        result.append(index.toString());
        result.append("]");
        return result.toString();
    }
}
