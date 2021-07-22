package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * Interface for recording facts at (pseudo-)assignments. It is used by the -Ainfer command-line
 * argument. The -Ainfer command-line argument is used by the whole-program-inference loop, but this
 * class does not implement that loop and its name {@code WholeProgramInference} is misleading.
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
   * Updates the parameter types of the constructor {@code constructorElt} based on the arguments in
   * {@code objectCreationNode}.
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
   * @param store the store just before the call
   */
  void updateFromObjectCreation(
      ObjectCreationNode objectCreationNode,
      ExecutableElement constructorElt,
      CFAbstractStore<?, ?> store);

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
   * @param methodElt the element of the method being invoked
   * @param store the store before the method call, used for inferring method preconditions
   */
  void updateFromMethodInvocation(
      MethodInvocationNode methodInvNode, ExecutableElement methodElt, CFAbstractStore<?, ?> store);

  /**
   * Updates the parameter types (including the receiver) of the method {@code methodTree} based on
   * the parameter types of the overridden method {@code overriddenMethod}.
   *
   * <p>For each formal parameter in methodElt:
   *
   * <ul>
   *   <li>If there is no stored annotated type for that parameter, then use the type of the
   *       corresponding parameter on the overridden method.
   *   <li>If there is a stored annotated type for that parameter, then its new type will be the LUB
   *       between the previous type and the type of the corresponding parameter on the overridden
   *       method.
   * </ul>
   *
   * @param methodTree the tree of the method that contains the parameter(s)
   * @param methodElt the element of the method
   * @param overriddenMethod the AnnotatedExecutableType of the overridden method
   */
  void updateFromOverride(
      MethodTree methodTree, ExecutableElement methodElt, AnnotatedExecutableType overriddenMethod);

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
   * @param lhs the node representing the formal parameter
   * @param rhs the node being assigned to the parameter in the method body
   * @param paramElt the formal parameter
   */
  void updateFromFormalParameterAssignment(
      LocalVariableNode lhs, Node rhs, VariableElement paramElt);

  /**
   * Updates the type of {@code field} based on an assignment of {@code rhs} to {@code field}. If
   * the field has a declaration annotation with the {@link IgnoreInWholeProgramInference}
   * meta-annotation, no type annotation will be inferred for that field.
   *
   * <p>If there is no stored entry for the field lhs, the entry will be created and its type will
   * be the type of rhs. If there is a stored entry/type for lhs, its new type will be the LUB
   * between the previous type and the type of rhs.
   *
   * @param field the field whose type will be refined. Must be either a FieldAccessNode or a
   *     LocalVariableNode whose element kind is FIELD.
   * @param rhs the expression being assigned to the field
   */
  void updateFromFieldAssignment(Node field, Node rhs);

  /**
   * Updates the type of {@code field} based on an assignment whose right-hand side has type {@code
   * rhsATM}. See more details at {@link #updateFromFieldAssignment}.
   *
   * @param lhsTree the tree for the field whose type will be refined
   * @param element the element for the field whose type will be refined
   * @param fieldName the name of the field whose type will be refined
   * @param rhsATM the type of the expression being assigned to the field
   */
  void updateFieldFromType(
      Tree lhsTree, Element element, String fieldName, AnnotatedTypeMirror rhsATM);

  /**
   * Updates the return type of the method {@code methodTree} based on {@code returnedExpression}.
   * Also updates the return types of any methods that this method overrides that are available as
   * source code.
   *
   * <p>If there is no stored annotated return type for the method methodTree, then the type of the
   * return expression will be added to the return type of that method. If there is a stored
   * annotated return type for the method methodTree, its new type will be the LUB between the
   * previous type and the type of the return expression.
   *
   * @param retNode the node that contains the expression returned
   * @param classSymbol the symbol of the class that contains the method
   * @param methodTree the tree of the method whose return type may be updated
   * @param overriddenMethods the methods that the given method return overrides, indexed by the
   *     annotated type of the superclass in which each method is defined
   */
  void updateFromReturn(
      ReturnNode retNode,
      ClassSymbol classSymbol,
      MethodTree methodTree,
      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods);

  /**
   * Updates the preconditions or postconditions of the current method, from a store.
   *
   * @param methodElement the method or constructor whose preconditions or postconditions to update
   * @param preOrPost whether to update preconditions or postconditions
   * @param store the store at the method's entry or normal exit, for reading types of expressions
   */
  void updateContracts(
      Analysis.BeforeOrAfter preOrPost,
      ExecutableElement methodElement,
      CFAbstractStore<?, ?> store);

  /**
   * Updates a method to add a declaration annotation.
   *
   * @param methodElt the method to annotate
   * @param anno the declaration annotation to add to the method
   */
  void addMethodDeclarationAnnotation(ExecutableElement methodElt, AnnotationMirror anno);

  /**
   * Writes the inferred results to a file. Ideally, it should be called at the end of the
   * type-checking process. In practice, it is called after each class, because we don't know which
   * class will be the last one in the type-checking process.
   *
   * @param format the file format in which to write the results
   * @param checker the checker from which this method is called, for naming stub files
   */
  void writeResultsToFile(OutputFormat format, BaseTypeChecker checker);

  /**
   * Performs any preparation required for inference on Elements of a class. Should be called on
   * each toplevel class declaration in a compilation unit before processing it.
   *
   * @param classTree the class to preprocess
   */
  void preprocessClassTree(ClassTree classTree);

  /** The kinds of output that whole-program inference can produce. */
  enum OutputFormat {
    /**
     * Output the results of whole-program inference as a stub file that can be parsed back into the
     * Checker Framework by the Stub Parser.
     */
    STUB(),

    /**
     * Output the results of whole-program inference as a Java annotation index file. The Annotation
     * File Utilities project contains code for reading and writing .jaif files.
     */
    JAIF(),

    /**
     * Output the results of whole-program inference as an ajava file that can be read in using the
     * -Aajava option.
     */
    AJAVA(),
  }
}
