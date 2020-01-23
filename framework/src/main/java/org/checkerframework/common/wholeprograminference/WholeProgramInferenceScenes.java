package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.util.JVMNames;

/**
 * WholeProgramInferenceScenes is an implementation of {@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInference} that uses a storage
 * class ({@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesStorage}) that
 * manipulates {@link scenelib.annotations.el.AScene}s to perform whole-program inference, and
 * writes them out to a .jaif file at the end.
 *
 * <p>Calling an update* method replaces the currently-stored type for an element in a {@link
 * scenelib.annotations.el.AScene}, if any, by the LUB of it and the update method's argument.
 *
 * <p>This class does not perform inference for an element if the element has explicit annotations:
 * calling an update* method on an explicitly annotated field, method return, or method parameter
 * has no effect.
 *
 * <p>In addition, whole program inference ignores inferred types in a few scenarios. When
 * discovering a use, if:
 *
 * <ol>
 *   <li>The inferred type of an element that should be written into a .jaif file is a subtype of
 *       the upper bounds of this element's currently-written type on the source code.
 *   <li>The annotation annotates a {@code null} literal, except when doing inference for the
 *       NullnessChecker. (The rationale for this is that {@code null} is a frequently-used default
 *       value, and it would be undesirable to compute any inferred type if {@code null} were the
 *       only value passed as an argument.)
 * </ol>
 *
 * When outputting a .jaif file, if:
 *
 * <ol>
 *   <li>The @Target annotation does not permit the annotation to be written at this location.
 *   <li>The inferred annotation has the @InvisibleQualifier meta-annotation.
 *   <li>The inferred annotation would be the same annotation applied via defaulting &mdash; that
 *       is, if omitting it has the same effect as writing it.
 * </ol>
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

    /** The interface to the AScene library itself, which stores the inferred annotations. */
    private final WholeProgramInferenceScenesStorage storage;

    /**
     * Default constructor.
     *
     * @param ignoreNullAssignments indicates whether assignments where the rhs is null should be
     *     ignored
     */
    public WholeProgramInferenceScenes(boolean ignoreNullAssignments) {
        storage = new WholeProgramInferenceScenesStorage(ignoreNullAssignments);
    }

    @Override
    public void updateFromObjectCreation(
            ObjectCreationNode objectCreationNode,
            ExecutableElement constructorElt,
            AnnotatedTypeFactory atf) {

        // do not infer types for code that isn't presented as source
        if (ElementUtils.isElementFromByteCode(constructorElt)) {
            return;
        }

        String className = getEnclosingClassName(constructorElt);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath);
        String methodName = JVMNames.getJVMMethodName(constructorElt);
        AMethod method = clazz.methods.getVivify(methodName);

        List<Node> arguments = objectCreationNode.getArguments();
        updateInferredExecutableParameterTypes(constructorElt, atf, jaifPath, method, arguments);
    }

    @Override
    public void updateFromMethodInvocation(
            MethodInvocationNode methodInvNode,
            Tree receiverTree,
            ExecutableElement methodElt,
            AnnotatedTypeFactory atf) {

        // do not infer types for code that isn't presented as source
        if (ElementUtils.isElementFromByteCode(methodElt)) {
            return;
        }

        String className = getEnclosingClassName(methodElt);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath);

        String methodName = JVMNames.getJVMMethodName(methodElt);
        AMethod method = clazz.methods.getVivify(methodName);

        List<Node> arguments = methodInvNode.getArguments();
        updateInferredExecutableParameterTypes(methodElt, atf, jaifPath, method, arguments);
    }

    /** Helper method for updating parameter types based on calls to a method or constructor. */
    private void updateInferredExecutableParameterTypes(
            ExecutableElement methodElt,
            AnnotatedTypeFactory atf,
            String jaifPath,
            AMethod method,
            List<Node> arguments) {
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
            AField param = method.parameters.getVivify(i);
            storage.updateAnnotationSetInScene(
                    param.type, atf, jaifPath, argATM, paramATM, TypeUseLocation.PARAMETER);
        }
    }

    @Override
    public void updateFromOverride(
            MethodTree methodTree,
            ExecutableElement methodElt,
            AnnotatedExecutableType overriddenMethod,
            AnnotatedTypeFactory atf) {

        // do not infer types for code that isn't presented as source
        if (ElementUtils.isElementFromByteCode(methodElt)) {
            return;
        }

        String className = getEnclosingClassName(methodElt);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath);
        String methodName = JVMNames.getJVMMethodName(methodElt);
        AMethod method = clazz.methods.getVivify(methodName);

        for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(ve);

            AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
            AField param = method.parameters.getVivify(i);
            storage.updateAnnotationSetInScene(
                    param.type, atf, jaifPath, argATM, paramATM, TypeUseLocation.PARAMETER);
        }

        AnnotatedDeclaredType argADT = overriddenMethod.getReceiverType();
        if (argADT != null) {
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(methodTree).getReceiverType();
            if (paramATM != null) {
                AField receiver = method.receiver;
                storage.updateAnnotationSetInScene(
                        receiver.type, atf, jaifPath, argADT, paramATM, TypeUseLocation.RECEIVER);
            }
        }
    }

    @Override
    public void updateFromLocalAssignment(
            LocalVariableNode lhs,
            Node rhs,
            ClassTree classTree,
            MethodTree methodTree,
            AnnotatedTypeFactory atf) {

        // do not infer types for code that isn't presented as source
        if (ElementUtils.isElementFromByteCode(lhs.getElement())) {
            return;
        }

        String className = getEnclosingClassName(lhs);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath);
        String methodName = JVMNames.getJVMMethodName(methodTree);
        AMethod method = clazz.methods.getVivify(methodName);

        List<? extends VariableTree> params = methodTree.getParameters();
        // Look-up parameter by name:
        for (int i = 0; i < params.size(); i++) {
            VariableTree vt = params.get(i);
            if (vt.getName().contentEquals(lhs.getName())) {
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
                AField param = method.parameters.getVivify(i);
                storage.updateAnnotationSetInScene(
                        param.type, atf, jaifPath, argATM, paramATM, TypeUseLocation.PARAMETER);
                break;
            }
        }
    }

    @Override
    public void updateFromFieldAssignment(
            FieldAccessNode lhs, Node rhs, ClassTree classTree, AnnotatedTypeFactory atf) {

        // do not infer types for code that isn't presented as source
        if (ElementUtils.isElementFromByteCode(lhs.getElement())) {
            return;
        }

        // If the inferred field has a declaration annotation with the
        // @IgnoreInWholeProgramInference meta-annotation, exit this routine.
        if (atf.getDeclAnnotation(lhs.getElement(), IgnoreInWholeProgramInference.class) != null
                || atf.getDeclAnnotationWithMetaAnnotation(
                                        lhs.getElement(), IgnoreInWholeProgramInference.class)
                                .size()
                        > 0) {
            return;
        }

        String className = getEnclosingClassName(lhs.getElement());
        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath);

        AField field = clazz.fields.getVivify(lhs.getFieldName());
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        // TODO: For a primitive such as long, this is yielding just @GuardedBy rather than
        // @GuardedBy({}).
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());
        storage.updateAnnotationSetInScene(
                field.type, atf, jaifPath, rhsATM, lhsATM, TypeUseLocation.FIELD);
    }

    @Override
    public void updateFromReturn(
            ReturnNode retNode,
            ClassSymbol classSymbol,
            MethodTree methodTree,
            AnnotatedTypeFactory atf) {

        // do not infer types for code that isn't presented as source
        if (methodTree == null
                || ElementUtils.isElementFromByteCode(
                        TreeUtils.elementFromDeclaration(methodTree))) {
            return;
        }

        // See Issue 682
        // https://github.com/typetools/checker-framework/issues/682
        if (classSymbol == null) { // TODO: Handle anonymous classes.
            return;
        }
        String className = classSymbol.flatname.toString();

        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath);

        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodName(methodTree));
        // Method return type
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(methodTree).getReturnType();
        // Type of the expression returned
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(retNode.getTree().getExpression());
        storage.updateAnnotationSetInScene(
                method.returnType, atf, jaifPath, rhsATM, lhsATM, TypeUseLocation.RETURN);
    }

    /** Write all modified scenes into .jaif files. */
    @Override
    public void saveResults() {
        storage.writeScenesToJaif();
    }

    /**
     * Returns the "flatname" of the class enclosing {@code localVariableNode}
     *
     * @param localVariableNode the {@link LocalVariableNode}
     * @return the "flatname" of the class enclosing {@code localVariableNode}
     */
    private String getEnclosingClassName(LocalVariableNode localVariableNode) {
        return ((ClassSymbol) ElementUtils.enclosingClass(localVariableNode.getElement()))
                .flatName()
                .toString();
    }

    /**
     * Returns the "flatname" of the class enclosing {@code executableElement}
     *
     * @param executableElement the ExecutableElement
     * @return the "flatname" of the class enclosing {@code executableElement}
     */
    private String getEnclosingClassName(ExecutableElement executableElement) {
        return ((MethodSymbol) executableElement).enclClass().flatName().toString();
    }

    /**
     * Returns the "flatname" of the class enclosing {@code variableElement}
     *
     * @param variableElement the VariableElement
     * @return the "flatname" of the class enclosing {@code variableElement}
     */
    private String getEnclosingClassName(VariableElement variableElement) {
        return ((VarSymbol) variableElement).enclClass().flatName().toString();
    }
}
