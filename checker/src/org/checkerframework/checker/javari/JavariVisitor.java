package org.checkerframework.checker.javari;


import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.checkerframework.checker.javari.qual.Mutable;
import org.checkerframework.checker.javari.qual.Assignable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;


/**
 * A type-checking visitor for the Javari mutability annotations
 * ({@code @ReadOnly}, {@code @Mutable} and {@code @Assignable}) that
 * extends BaseTypeVisitor.
 *
 * @see BaseTypeVisitor
 */
public class JavariVisitor extends BaseTypeVisitor<JavariAnnotatedTypeFactory> {

    /**
     * Creates a new visitor for type-checking the Javari mutability
     * annotations.
     *
     * @param checker the {@link JavariChecker} to use
     */
    public JavariVisitor(BaseTypeChecker checker) {
        super(checker);
        checkForAnnotatedJdk();
    }

    /**
     * Ensures the class type is not {@code @PolyRead} outside a
     * {@code @PolyRead} context.
     */
    @Override
    public Void visitClass(ClassTree node, Void p) {
        if (atypeFactory.fromClass(node).hasEffectiveAnnotation(atypeFactory.POLYREAD))
                /* TODO: what was this meant to do?
            && !atypeFactory.getSelfType(node).hasEffectiveAnnotation(atypeFactory.POLYREAD)*/
            checker.report(Result.failure("polyread.type"), node);

        return super.visitClass(node, p);
    }

   /**
    * Checks whether the variable represented by the given type and
    * tree can be assigned, causing a checker error otherwise.
    */
    @Override
    protected void checkAssignability(AnnotatedTypeMirror varType, Tree varTree) {
        if (!TreeUtils.isExpressionTree(varTree))
            return;
        Element varElt = TreeUtils.elementFromUse((ExpressionTree) varTree);
        if (varElt != null && atypeFactory.getDeclAnnotation(varElt, Assignable.class) != null)
            return;

        boolean variableLocalField =
            atypeFactory.isMostEnclosingThisDeref((ExpressionTree) varTree)
            && varElt != null
            && varElt.getKind().isField();
        // visitState.getMethodTree() is null when in static initializer block
        boolean inConstructor =  visitorState.getMethodTree() == null
            || TreeUtils.isConstructor(visitorState.getMethodTree());
        AnnotatedDeclaredType mReceiver = atypeFactory.getSelfType(varTree);

        if (variableLocalField && !inConstructor && !mReceiver.hasEffectiveAnnotation(atypeFactory.MUTABLE))
            checker.report(Result.failure("ro.field"), varTree);

        if (varTree.getKind() == Tree.Kind.MEMBER_SELECT
            && !atypeFactory.isMostEnclosingThisDeref((ExpressionTree)varTree)) {
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType((ExpressionTree)varTree);
            if (receiver != null && !receiver.hasEffectiveAnnotation(atypeFactory.MUTABLE))
                checker.report(Result.failure("ro.field"), varTree);
        }

        if (varTree.getKind() == Tree.Kind.ARRAY_ACCESS) {
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType((ExpressionTree)varTree);
            if (receiver != null && !receiver.hasEffectiveAnnotation(atypeFactory.MUTABLE))
                checker.report(Result.failure("ro.element"), varTree);
        }

    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        // f exception parameters are forced to be @ReadOnly, then unannotated
        // code might not type check. For example:
        /*
         * catch( @ReadOnly Exception e) {
         *     Exception e2 = e;  // incompatible types, expected @Mutable
         *     throw new RuntimeException("message'", e) // incompatible types, expected @Mutable
         * }
         */
        //This is sound because an throw exception must be @Mutable
        return Collections.singleton(atypeFactory.MUTABLE);
    }

    /**
     * Always true; no type validity checking is made by the BaseTypeVisitor.
     *
     * @see BaseTypeVisitor
     */
    @Override
    public boolean isValidUse(AnnotatedDeclaredType elemType, AnnotatedDeclaredType useType, Tree tree) {
        return true;
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType useType, Tree tree) {
        if (useType.hasAnnotation(atypeFactory.QREADONLY)
                || useType.hasAnnotation(atypeFactory.READONLY)
                || useType.hasAnnotation(atypeFactory.POLYREAD)) {
            return false;
        }
        return super.isValidUse(useType, tree);
    }
}
