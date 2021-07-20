package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.type.TypeMirror;

/** Stands for any expression that the Dataflow Framework lacks explicit support for. */
@UsesObjectEquals
public class Unknown extends JavaExpression {

    /** String representation of the expression that has no corresponding {@code JavaExpression}. */
    private final String originalExpression;
    /**
     * Create a new Unknown JavaExpression.
     *
     * @param type the Java type of this
     */
    public Unknown(TypeMirror type) {
        this(type, "?");
    }

    /**
     * Create a new Unknown JavaExpression.
     *
     * @param type the Java type of this
     * @param originalExpression String representation of the expression that has no corresponding
     *     {@code JavaExpression}
     */
    public Unknown(TypeMirror type, String originalExpression) {
        super(type);
        this.originalExpression = originalExpression;
    }

    /**
     * Create a new Unknown JavaExpression.
     *
     * @param tree a tree that does not have a corresponding {@code JavaExpression}
     */
    public Unknown(Tree tree) {
        this(TreeUtils.typeOf(tree), TreeUtils.toStringTruncated(tree, 40));
    }

    /**
     * Create a new Unknown JavaExpression.
     *
     * @param node a node that does not have a corresponding {@code JavaExpression}
     */
    public Unknown(Node node) {
        this(node.getType(), node.toString());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this;
    }

    // Overridden to avoid an error "overrides equals, but does not override hashCode"
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return originalExpression;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        return getClass() == clazz;
    }

    @Override
    public boolean isDeterministic(AnnotationProvider provider) {
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
    public boolean syntacticEquals(JavaExpression je) {
        return this == je;
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return this.syntacticEquals(other);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return true;
    }

    @Override
    public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
        return visitor.visitUnknown(this, p);
    }
}
