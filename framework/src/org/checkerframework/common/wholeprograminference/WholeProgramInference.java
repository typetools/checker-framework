package org.checkerframework.common.wholeprograminference;

import javax.lang.model.element.ExecutableElement;

import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

/**
 * Interface for a whole-program inference implementation.
 * @author pbsf
 */
public interface WholeProgramInference {

    /**
     * Updates the parameter types of the method methodElt.
     * @param methodInvNode the node representing a method invocation.
     * @param receiverTree the Tree of the class that contains the method being
     * invoked.
     * @param methodElt the element of the method being invoked.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method parameters' types.
     */
    public void updateInferredMethodParametersTypes(
            MethodInvocationNode methodInvNode, Tree receiverTree,
            ExecutableElement methodElt, AnnotatedTypeFactory atf);

    /**
     * Updates the parameter type represented by lhs of the method methodTree.
     * @param lhs the node representing the parameter.
     * @param rhs the node being assigned to the parameter.
     * @param classTree the tree of the class that contains the parameter.
     * @param methodTree the tree of the method that contains the parameter.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the parameter type.
     */
    public void updateInferredParameterType(LocalVariableNode lhs,
            Node rhs, ClassTree classTree, MethodTree methodTree,
            AnnotatedTypeFactory atf);

    /**
     * Updates the type of the field lhs.
     * @param lhs the field whose type will be refined.
     * @param rhs the expression being assigned to the field.
     * @param classTree the ClassTree for the enclosing class of the assignment.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the field's type.
     */
    public void updateInferredFieldType(Node lhs, Node rhs,
            ClassTree classTree, AnnotatedTypeFactory atf);

    /**
     * Updates the return type of the method methodTree.
     * @param retNode the node that contains the expression returned.
     * @param classSymbol the symbol of the class that contains the method.
     * @param methodTree the tree of the method whose return type
     * may be updated.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method's return type.
     */
    public void updateInferredMethodReturnType(ReturnNode retNode,
            ClassSymbol classSymbol, MethodTree methodTree,
            AnnotatedTypeFactory atf);

    /**
     * Saves the inferred results. Ideally should be called at the end of the
     * type-checking process.
     */
    public void saveResults();
}