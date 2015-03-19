package org.checkerframework.qualframework.base;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualValue;
import org.checkerframework.qualframework.util.ExtendedParameterDeclaration;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/**
 * Used to compute the qualified type of a {@link Tree} or {@link Element}.
 * Also includes hooks that can be overridden to modify typechecking behavior
 * in type system-specific ways.
 */
public interface QualifiedTypeFactory<Q> {
    /** Gets the qualified type of an {@link Element}. */
    QualifiedTypeMirror<Q> getQualifiedType(Element element);
    /** Gets the qualified type of an AST node. */
    QualifiedTypeMirror<Q> getQualifiedType(Tree tree);
    /** Gets the qualified type from a type in {@link Tree} form. */
    QualifiedTypeMirror<Q> getQualifiedTypeFromTypeTree(Tree typeTree);

    /** Gets the qualified types of the bounds of a type parameter, identified
     * by its {@link Element}. */
    QualifiedTypeParameterBounds<Q> getQualifiedTypeParameterBounds(ExtendedParameterDeclaration etm);

    /** Gets the {@link QualifierHierarchy} used with this type system. */
    QualifierHierarchy<Q> getQualifierHierarchy();
    /** Gets the {@link TypeHierarchy} used with this type system. */
    TypeHierarchy<Q> getTypeHierarchy();
    /** Gets the {@link QualifiedTypes} helper object used with this type
     * system. */
    QualifiedTypes<Q> getQualifiedTypes();

    /**
     * Hook for customizing the behavior of <code>directSuperTypes</code>.
     *
     * @param subtype
     *      the target of the <code>directSuperTypes</code> call
     * @param supertypes
     *      the supertypes produced by the default
     *      <code>directSuperTypes</code> implementation
     * @return
     *      a copy of <code>supertypes</code> after applying checker-specific
     *      adjustments
     */
    List<QualifiedTypeMirror<Q>> postDirectSuperTypes(QualifiedTypeMirror<Q> subtype, List<? extends QualifiedTypeMirror<Q>> supertypes);

    /** Hook for customizing the behavior of <code>asMemberOf</code>.
     *
     * @param memberType
     *      the type of the element being accessed, according to the default
     *      <code>asMemberOf</code> implementation
     * @param receiverType
     *      the type of the object instance whose element is being accessed
     * @param memberElement
     *      the element being accessed
     * @return
     *      a copy of <code>memberType</code> after applying checker-specific
     *      adjustments
     */
    QualifiedTypeMirror<Q> postAsMemberOf(QualifiedTypeMirror<Q> memberType, QualifiedTypeMirror<Q> receiverType, Element memberElement);

    /** Hook for customizing type parameter inference for methods.
     *
     * @param tree
     *      the AST node for the method call
     * @return
     *      the type of the called method with all parameters instantiated, and
     *      a list of the type used to instantiate each parameter
     */
    Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> methodFromUse(MethodInvocationTree tree);

    /**
     * @see QualifiedTypeFactory#methodFromUse(MethodInvocationTree)
     */
    Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> methodFromUse(ExpressionTree tree,
            ExecutableElement methodElt, QualifiedTypeMirror<Q> receiverType);

    /**
     * Hook for customizing type parameter inference for constructors.
     *
     * @param tree
     *      the AST node for the constructor call
     * @return
     *      the type of the called constructor with all parameters
     *      instantiated, and a list of the type used to instantiate each
     *      parameter
     */
    Pair<QualifiedExecutableType<Q>, List<QualifiedTypeMirror<Q>>> constructorFromUse(NewClassTree tree);

    /**
     * Create the {@link Analysis} to configure dataflow.
     *
     * @param fieldValues The initial field values
     * @return The {@link QualAnalysis} to use
     */
    QualAnalysis<Q> createFlowAnalysis(List<Pair<VariableElement, QualValue<Q>>> fieldValues);

    /**
     * @param node The @{@link Tree} to look up the {@link TreePath} for
     * @return The {@link TreePath}
     */
    TreePath getPath(Tree node);

    /**
     * Returns the receiver type of the expression tree, or null if it does not exist.
     *
     * The only trees that could potentially have a receiver are:
     * <ul>
     *  <li> ArrayAccessTree
     *  <li> IdentifierTree (whose receivers are usually self type)
     *  <li> MethodInvocationTree
     *  <li> MemberSelectTree
     * </ul>
     *
     * @param expression The expression for which to determine the receiver type
     * @return  the type of the receiver of this expression
     */
    QualifiedTypeMirror<Q> getReceiverType(ExpressionTree expression);

    /**
     * Get an {@link ExtendedTypeMirror} for an {@link Element} that has all the Annotations
     * that were located on the element in source code or in stub files.
     *
     * @param element The {@link Element}
     * @return The {@link ExtendedTypeMirror}
     */
    ExtendedTypeMirror getDecoratedElement(Element element);

    /**
     * @return a TypeVariableSubstitutor
     */
    TypeVariableSubstitutor<Q> createTypeVariableSubstitutor();

    Set<AnnotationMirror> getDeclAnnotations(Element elt);
}
