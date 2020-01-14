package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * Interface for a whole-program inference implementation.
 *
 * <p>This interface has update* methods that should be called at certain (pseudo-)assignments, and
 * they may update the type of the LHS of the (pseudo-)assignment based on the type of the RHS. In
 * case the element on the LHS already had an inferred type, its new type will be the LUB between
 * the previous and new types.
 *
 * @checker_framework.manual #whole-program-inference Whole-program inference
 */
public interface WholeProgramInference {

    /**
     * Updates the parameter types of the constructor {@code constructorElt} based on the arguments
     * in {@code objectCreationNode}.
     *
     * <p>For each parameter in constructorElt:
     *
     * <ul>
     *   <li>If there is no stored annotated type for that parameter, then use the type of the
     *       corresponding argument in the object creation call objectCreationNode.
     *   <li>If there was a stored annotated type for that parameter, then its new type will be the
     *       LUB between the previous type and the type of the corresponding argument in the object
     *       creation call.
     * </ul>
     *
     * @param objectCreationNode the Node that invokes the constructor
     * @param constructorElt the Element of the constructor
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used to update the constructor's parameters' types
     */
    void updateFromObjectCreation(
            ObjectCreationNode objectCreationNode,
            ExecutableElement constructorElt,
            AnnotatedTypeFactory atf);

    /**
     * Updates the parameter types of the method {@code methodElt} based on the arguments in the
     * method invocation {@code methodInvNode}.
     *
     * <p>For each formal parameter in methodElt (including the receiver):
     *
     * <ul>
     *   <li>If there is no stored annotated type for that parameter, then use the type of the
     *       corresponding argument in the method call methodInvNode.
     *   <li>If there was a stored annotated type for that parameter, then its new type will be the
     *       LUB between the previous type and the type of the corresponding argument in the method
     *       call.
     * </ul>
     *
     * @param methodInvNode the node representing a method invocation
     * @param receiverTree the Tree of the class that contains the method being invoked
     * @param methodElt the element of the method being invoked
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used to update the method parameters' types
     */
    void updateFromMethodInvocation(
            MethodInvocationNode methodInvNode,
            Tree receiverTree,
            ExecutableElement methodElt,
            AnnotatedTypeFactory atf);

    /**
     * Updates the parameter types (including the receiver) of the method {@code methodTree} based
     * on the parameter types of the overridden method {@code overriddenMethod}.
     *
     * <p>For each formal parameter in methodElt:
     *
     * <ul>
     *   <li>If there is no stored annotated type for that parameter, then use the type of the
     *       corresponding parameter on the overridden method.
     *   <li>If there is a stored annotated type for that parameter, then its new type will be the
     *       LUB between the previous type and the type of the corresponding parameter on the
     *       overridden method.
     * </ul>
     *
     * @param methodTree the tree of the method that contains the parameter(s)
     * @param methodElt the element of the method
     * @param overriddenMethod the AnnotatedExecutableType of the overridden method
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used to update the parameter type
     */
    void updateFromOverride(
            MethodTree methodTree,
            ExecutableElement methodElt,
            AnnotatedExecutableType overriddenMethod,
            AnnotatedTypeFactory atf);

    /**
     * Updates the type of {@code lhs} based on an assignment of {@code rhs} to {@code lhs}.
     *
     * <ul>
     *   <li>If there is no stored annotated type for lhs, then use the type of the corresponding
     *       argument in the method call methodInvNode.
     *   <li>If there is a stored annotated type for lhs, then its new type will be the LUB between
     *       the previous type and the type of the corresponding argument in the method call.
     * </ul>
     *
     * @param lhs the node representing the local variable, such as a formal parameter
     * @param rhs the node being assigned to the parameter in the method body
     * @param classTree the tree of the class that contains the parameter
     * @param methodTree the tree of the method that contains the parameter
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used to update the parameter type
     */
    void updateFromLocalAssignment(
            LocalVariableNode lhs,
            Node rhs,
            ClassTree classTree,
            MethodTree methodTree,
            AnnotatedTypeFactory atf);

    /**
     * Updates the type of {@code field} based on an assignment of {@code rhs} to {@code field}. If
     * the field has a declaration annotation with the {@link IgnoreInWholeProgramInference}
     * meta-annotation, no type annotation will be inferred for that field.
     *
     * <p>If there is no stored entry for the field lhs, the entry will be created and its type will
     * be the type of rhs. If there is a stored entry/type for lhs, its new type will be the LUB
     * between the previous type and the type of rhs.
     *
     * @param field the field whose type will be refined
     * @param rhs the expression being assigned to the field
     * @param classTree the ClassTree for the enclosing class of the assignment
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used to update the field's type
     */
    void updateFromFieldAssignment(
            FieldAccessNode field, Node rhs, ClassTree classTree, AnnotatedTypeFactory atf);

    /**
     * Updates the return type of the method {@code methodTree} based on {@code returnedExpression}.
     *
     * <p>If there is no stored annotated return type for the method methodTree, then the type of
     * the return expression will be added to the return type of that method. If there is a stored
     * annotated return type for the method methodTree, its new type will be the LUB between the
     * previous type and the type of the return expression.
     *
     * @param retNode the node that contains the expression returned
     * @param classSymbol the symbol of the class that contains the method
     * @param methodTree the tree of the method whose return type may be updated
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used to update the method's return type
     */
    void updateFromReturn(
            ReturnNode retNode,
            ClassSymbol classSymbol,
            MethodTree methodTree,
            AnnotatedTypeFactory atf);

    /**
     * Saves the inferred results. Ideally should be called at the end of the type-checking process.
     */
    void saveResults();
}
