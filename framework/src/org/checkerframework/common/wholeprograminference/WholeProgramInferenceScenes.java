package org.checkerframework.common.wholeprograminference;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;

import annotations.el.AClass;
import annotations.el.AField;
import annotations.el.AMethod;
import annotations.util.JVMNames;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.ClassType;

/**
 * WholeProgramInferenceScenes is an implementation of
 * {@link org.checkerframework.common.wholeprograminference.WholeProgramInference}
 * that uses a helper class
 * ({@link org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesHelper})
 * that manipulates .jaif files to perform whole-program inference.
 * <p>
 * Calling an update* method
 * ({@link #updateInferredFieldType updateInferredFieldType},
 * {@link #updateInferredMethodParameterTypes updateInferredMethodParameterTypes},
 * {@link #updateInferredParameterType updateInferredParameterType}, or
 * {@link #updateInferredMethodReturnType updateInferredMethodReturnType})
 * replaces the currently-stored type for an element in a {@link annotations.el.AScene}, if any,
 * by the LUB of it and the update method's argument.
 * <p>
 * This class does not perform inference for an element if the element has
 * explicit annotations:  an update* method ignores an
 * explicitly annotated field, method return, or method parameter when
 * passed as an argument.
 * <p>
 * In addition, whole program inference ignores inferred types in a few scenarios.
 * When discovering a use, if:
 * <ol>
 *   <li>The inferred type of an element that should be written into a .jaif
 *       file is a subtype of the upper bounds of this element's currently-written
 *       type on the source code.</li>
 *   <li>The annotation annotates a <code>null</code> literal, except when
 *       doing inference for the NullnessChecker.  (The rationale for this
 *       is that <code>null</code> is a frequently-used default value, and
 *       it would be undesirable to compute any inferred type if
 *       <code>null</code> were the only value passed as an argument.)</li>
 * </ol>
 * When outputting a .jaif file, if:
 * <ol>
 *   <li>The @Target annotation does not permit the annotation to be
 *       written at this location.</li>
 *   <li>The inferred has the @InvisibleQualifier meta-annotation.</li>
 *   <li>The resulting type would be defaulted or implicited &mdash; that is, if
 *       omitting it has the same effect as writing it.</li>
 *   <li>Special case: The
 *       {@link org.checkerframework.qualframework.base.TypeMirrorConverter.Key}
 *       won't be written into .jaif. (This will probably change once we support
 *       type-checkers that use a CheckerAdapter.)</li>
 * </ol>
 *  @author pbsf
 */
//  TODO: We could add an option to update the type of explicitly annotated
//  elements, but this currently is not recommended since the
//  insert-annotations-to-source tool, which adds annotations from .jaif files
//  into source code, adds annotations on top of existing
//  annotations. See https://github.com/typetools/annotation-tools/issues/105 .
//  TODO: Ensure that annotations are inserted deterministically into .jaif
//  files. This is important for debugging and comparison; otherwise running
//  the whole-program inference on the same set of files can yield different
//  results (order of annotations).
public class WholeProgramInferenceScenes implements WholeProgramInference {

    private final WholeProgramInferenceScenesHelper helper;

    public WholeProgramInferenceScenes(boolean ignoreNullAssignments) {
        helper = new WholeProgramInferenceScenesHelper(ignoreNullAssignments);
    }

    /**
     * Updates the parameter types of the constructor created by objectCreationNode
     * based on arguments to the constructor.
     * <p>
     * For each parameter in constructorElt:
     *   <ul>
     *     <li>If the Scene does not contain an annotated type for that
     *     parameter, then the type of the respective value passed as argument
     *     in the object creation call objectCreationNode will be added to the
     *     parameter in the Scene.</li>
     *     <li>If the Scene previously contained an annotated type for that
     *     parameter, then its new type will be the LUB between the previous
     *     type and the type of the respective value passed as argument in the
     *     object creation call.</li>
     *   </ul>
     * <p>
     * @param objectCreationNode the new Object() node.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the constructor's parameters' types.
     */
    @Override
    public void updateInferredConstructorParameterTypes(
            ObjectCreationNode objectCreationNode,
            ExecutableElement constructorElt, AnnotatedTypeFactory atf) {
        ClassSymbol classSymbol = getEnclosingClassSymbol(objectCreationNode.getTree());
        if (classSymbol == null) {
            // TODO: Handle anonymous classes.
            // See Issue 682
            // https://github.com/typetools/checker-framework/issues/682
            return;
        }

        String className = classSymbol.flatname.toString();
        String jaifPath = helper.getJaifPath(className);
        AClass clazz = helper.getAClass(className, jaifPath);
        String methodName = JVMNames.getJVMMethodName(constructorElt);
        AMethod method = clazz.methods.vivify(methodName);

        List<Node> arguments = objectCreationNode.getArguments();
        updateInferredExecutableParameterTypes(constructorElt, atf, jaifPath, method, arguments);

    }

    /**
     * Updates the parameter types of the method methodElt in the Scene of the
     * receiverTree's enclosing class based on the arguments to the method.
     * <p>
     * For each method parameter in methodElt:
     *   <ul>
     *     <li>If the Scene does not contain an annotated type for that
     *     parameter, then the type of the respective value passed as argument
     *     in the method call methodInvNode will be added to the parameter in
     *     the Scene.</li>
     *     <li>If the Scene previously contained an annotated type for that
     *     parameter, then its new type will be the LUB between the previous
     *     type and the type of the respective value passed as argument in the
     *     method call.</li>
     *   </ul>
     * <p>
     * @param methodInvNode the node representing a method invocation.
     * @param receiverTree the Tree of the class that contains the method being
     * invoked.
     * @param methodElt the element of the method being invoked.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method parameters' types.
     */
    @Override
    public void updateInferredMethodParameterTypes(
            MethodInvocationNode methodInvNode, Tree receiverTree,
            ExecutableElement methodElt, AnnotatedTypeFactory atf) {
        if (receiverTree == null) {
            // TODO: Method called from static context.
            // I struggled to obtain the ClassTree of a method called
            // from a static context and currently I'm ignoring it.
            // See Issue 682
            // https://github.com/typetools/checker-framework/issues/682
            return;
        }
        ClassSymbol classSymbol = getEnclosingClassSymbol(receiverTree);
        if (classSymbol == null) {
            // TODO: Handle anonymous classes.
            // Also struggled to obtain the ClassTree from an anonymous class.
            // Ignoring it for now.
            // See Issue 682
            // https://github.com/typetools/checker-framework/issues/682
            return;
        }
        // TODO: We must handle cases where the method is declared on a superclass.
        // Currently we are ignoring them. See ElementUtils#getSuperTypes.
        // See Issue 682
        // https://github.com/typetools/checker-framework/issues/682
        if (!classSymbol.getEnclosedElements().contains(methodElt)) return;

        String className = classSymbol.flatname.toString();
        String jaifPath = helper.getJaifPath(className);
        AClass clazz = helper.getAClass(className, jaifPath);

        String methodName = JVMNames.getJVMMethodName(methodElt);
        AMethod method = clazz.methods.vivify(methodName);

        List<Node> arguments = methodInvNode.getArguments();
        updateInferredExecutableParameterTypes(methodElt, atf, jaifPath, method, arguments);
    }

    /**
     * Helper method for updating parameter types based on calls to a method or constructor.
     */
    private void updateInferredExecutableParameterTypes(ExecutableElement methodElt, AnnotatedTypeFactory atf,
                                                        String jaifPath, AMethod method, List<Node> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(ve);

            Node arg = arguments.get(i);
            Tree treeNode = arg.getTree();
            if (treeNode == null) {
                // TODO: Handle variable-length list as parameter.
                // An ArrayCreationNode with a null tree is created when the
                // parameter is a variable-length list. We are ignoring it for now.
                // See Issue 682
                // https://github.com/typetools/checker-framework/issues/682
                continue;
            }
            AnnotatedTypeMirror argATM = atf.getAnnotatedType(treeNode);
            AField param = method.parameters.vivify(i);
            helper.updateAnnotationSetInScene(
                    param.type, atf, jaifPath, argATM, paramATM,
                    TypeUseLocation.PARAMETER);
        }
    }

    /**
     * Updates the parameter type represented by lhs of the method methodTree
     * in the Scene of the receiverTree's enclosing class based on assignments
     * to the parameter inside the method body.
     *   <ul>
     *     <li>If the Scene does not contain an annotated type for that
     *     parameter, then the type of the respective value passed as argument
     *     in the method call methodInvNode will be added to the parameter in
     *     the Scene.</li>
     *     <li>If the Scene previously contained an annotated type for that
     *     parameter, then its new type will be the LUB between the previous
     *     type and the type of the respective value passed as argument in the
     *     method call.</li>
     *   </ul>
     * <p>
     * @param lhs the node representing the parameter.
     * @param rhs the node being assigned to the parameter.
     * @param classTree the tree of the class that contains the parameter.
     * @param methodTree the tree of the method that contains the parameter.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the parameter type.
     */
    @Override
    public void updateInferredParameterType(
            LocalVariableNode lhs, Node rhs, ClassTree classTree,
            MethodTree methodTree, AnnotatedTypeFactory atf) {
        ClassSymbol classSymbol = getEnclosingClassSymbol(classTree, lhs);
        // TODO: Anonymous classes
        // See Issue 682
        // https://github.com/typetools/checker-framework/issues/682
        if (classSymbol == null) return;

        String className = classSymbol.flatname.toString();
        String jaifPath = helper.getJaifPath(className);
        AClass clazz = helper.getAClass(className, jaifPath);
        String methodName = JVMNames.getJVMMethodName(methodTree);
        AMethod method = clazz.methods.vivify(methodName);

        List<? extends VariableTree> params = methodTree.getParameters();
        // Look-up parameter by name:
        for (int i = 0; i < params.size(); i++) {
            VariableTree vt = params.get(i);
            if (vt.getName().toString().equals(lhs.getName())) {
                Tree treeNode = rhs.getTree();
                if (treeNode == null) {
                    // TODO: Handle variable-length list as parameter.
                    // An ArrayCreationNode with a null tree is created when the
                    // parameter is a variable-length list. We are ignoring it for now.
                    // See Issue 682
                    // https://github.com/typetools/checker-framework/issues/682
                    continue;
                }
                AnnotatedTypeMirror paramATM = atf.getAnnotatedType(vt);
                AnnotatedTypeMirror argATM = atf.getAnnotatedType(treeNode);
                AField param = method.parameters.vivify(i);
                helper.updateAnnotationSetInScene(
                        param.type, atf, jaifPath, argATM, paramATM,
                        TypeUseLocation.PARAMETER);
                break;
            }
        }
    }

    /**
     * Updates the type of the field lhs in the Scene of the class with
     * tree classTree. If the field has a declaration annotation with the
     * {@link IgnoreInWholeProgramInference} meta-annotation, no type annotation
     * will be inferred for that field.
     * <p>
     * If the Scene contains no entry for the field lhs,
     * the entry will be created and its type will be the type of rhs. If the
     * Scene previously contained an entry/type for lhs, its new type will be
     * the LUB between the previous type and the type of rhs.
     * <p>
     * @param lhs the field whose type will be refined.
     * @param rhs the expression being assigned to the field.
     * @param classTree the ClassTree for the enclosing class of the assignment.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the field's type.
     */
    @Override
    public void updateInferredFieldType(
            FieldAccessNode lhs, Node rhs, ClassTree classTree, AnnotatedTypeFactory atf) {
        ClassSymbol classSymbol = getEnclosingClassSymbol(classTree, lhs);
        // See Issue 682
        // https://github.com/typetools/checker-framework/issues/682
        if (classSymbol == null) return; // TODO: Handle anonymous classes.

        // TODO: We must handle cases where the field is declared on a superclass.
        // Currently we are ignoring them. See ElementUtils#getSuperTypes.
        // See Issue 682
        // https://github.com/typetools/checker-framework/issues/682
        if (!classSymbol.getEnclosedElements().contains(lhs.getElement())) return;

        // If the inferred field has a declaration annotation with the
        // @IgnoreInWholeProgramInference meta-annotation, exit this routine.
        for (AnnotationMirror declAnno : atf.getDeclAnnotations(
                InternalUtils.symbol(lhs.getTree()))) {
            Element elt = declAnno.getAnnotationType().asElement();
            if (elt.getAnnotation(IgnoreInWholeProgramInference.class) != null) {
                return;
            }
        }

        String className = classSymbol.flatname.toString();
        String jaifPath = helper.getJaifPath(className);
        AClass clazz = helper.getAClass(className, jaifPath);

        AField field = clazz.fields.vivify(lhs.getFieldName());
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());
        helper.updateAnnotationSetInScene(
                field.type, atf, jaifPath, rhsATM, lhsATM, TypeUseLocation.FIELD);
    }

    /**
     * Updates the return type of the method methodTree in the
     * Scene of the class with symbol classSymbol.
     * <p>
     * If the Scene does not contain an annotated return type for the method
     * methodTree, then the type of the value passed to the return expression
     * will be added to the return type of that method in the Scene.
     * If the Scene previously contained an annotated return type for the
     * method methodTree, its new type will be the LUB between the previous
     * type and the type of the value passed to the return expression.
     * <p>
     * @param retNode the node that contains the expression returned.
     * @param classSymbol the symbol of the class that contains the method.
     * @param methodTree the tree of the method whose return type
     * may be updated.
     * @param atf the annotated type factory of a given type system, whose
     * type hierarchy will be used to update the method's return type.
     */
    @Override
    public void updateInferredMethodReturnType(ReturnNode retNode,
            ClassSymbol classSymbol, MethodTree methodTree,
            AnnotatedTypeFactory atf) {
        // See Issue 682
        // https://github.com/typetools/checker-framework/issues/682
        if (classSymbol == null) return; // TODO: Handle anonymous classes.
        String className = classSymbol.flatname.toString();

        String jaifPath = helper.getJaifPath(className);
        AClass clazz = helper.getAClass(className, jaifPath);

        AMethod method = clazz.methods.vivify(JVMNames.getJVMMethodName(methodTree));
        // Method return type
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(methodTree).getReturnType();
        // Type of the expression returned
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(retNode.getTree().getExpression());
        helper.updateAnnotationSetInScene(
                method.returnType, atf, jaifPath, rhsATM, lhsATM,
                TypeUseLocation.RETURN);
    }

    /**
     * Write all modified scenes into .jaif files.
     */
    @Override
    public void saveResults() {
        helper.writeScenesToJaif();
    }

    /**
     * Returns the ClassSymbol of the class encapsulating
     * the node n passed as parameter.
     * <p>
     * If the receiver of field is an instance of "this", the implementation
     * obtains the ClassSymbol by using classTree. Otherwise, the ClassSymbol
     * is from the field's receiver.
     */
    // TODO: These methods below could be moved somewhere else.
    private ClassSymbol getEnclosingClassSymbol(
            ClassTree classTree, Node field) {
        Node receiverNode = null;
        if (field instanceof FieldAccessNode) {
            receiverNode = ((FieldAccessNode)field).getReceiver();
        } else if (field instanceof LocalVariableNode) {
            receiverNode = ((LocalVariableNode)field).getReceiver();
        } else {
            ErrorReporter.errorAbort("Unexpected type: " + field.getClass());
        }
        if ((receiverNode == null || receiverNode instanceof ImplicitThisLiteralNode)
                && classTree != null) {
            return (ClassSymbol) InternalUtils.symbol(classTree);
        }
        TypeMirror type = receiverNode.getType();
        if (type instanceof ClassType) {
            TypeSymbol tsym = ((ClassType) type).asElement();
            return tsym.enclClass();
        }
        return getEnclosingClassSymbol(receiverNode.getTree());
    }

    /**
     * Returns the ClassSymbol of the class encapsulating
     * tree passed as parameter.
     */
    private ClassSymbol getEnclosingClassSymbol(Tree tree) {
        Element symbol = InternalUtils.symbol(tree);
        if (symbol instanceof ClassSymbol) {
            return (ClassSymbol) symbol;
        } else if (symbol instanceof VarSymbol) {
            return ((VarSymbol)symbol).asType().asElement().enclClass();
        } else if (symbol instanceof MethodSymbol) {
            return ((MethodSymbol)symbol).enclClass();
        }
        return null;
    }
}
