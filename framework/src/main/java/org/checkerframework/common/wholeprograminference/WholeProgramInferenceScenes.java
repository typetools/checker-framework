package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
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
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.BugInCF;
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
        if (!ElementUtils.isElementFromSourceCode(constructorElt)) {
            return;
        }

        String className = getEnclosingClassName(constructorElt);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz =
                storage.getAClass(className, jaifPath, ((MethodSymbol) constructorElt).enclClass());
        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(constructorElt));
        method.setFieldsFromMethodElement(constructorElt);

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
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String className = getEnclosingClassName(methodElt);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz =
                storage.getAClass(className, jaifPath, ((MethodSymbol) methodElt).enclClass());

        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        method.setFieldsFromMethodElement(methodElt);

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
            AField param =
                    method.vivifyAndAddTypeMirrorToParameter(
                            i, argATM.getUnderlyingType(), ve.getSimpleName());
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
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String className = getEnclosingClassName(methodElt);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz =
                storage.getAClass(className, jaifPath, ((MethodSymbol) methodElt).enclClass());
        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        method.setFieldsFromMethodElement(methodElt);

        for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(ve);

            AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
            AField param =
                    method.vivifyAndAddTypeMirrorToParameter(
                            i, argATM.getUnderlyingType(), ve.getSimpleName());
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
        if (!isElementFromSourceCode(lhs)) {
            return;
        }

        String className = getEnclosingClassName(lhs);
        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath);
        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodTree);
        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        method.setFieldsFromMethodElement(methodElt);

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
                VariableElement ve = TreeUtils.elementFromDeclaration(vt);
                AField param =
                        method.vivifyAndAddTypeMirrorToParameter(
                                i, argATM.getUnderlyingType(), ve.getSimpleName());
                storage.updateAnnotationSetInScene(
                        param.type, atf, jaifPath, argATM, paramATM, TypeUseLocation.PARAMETER);
                break;
            }
        }
    }

    @Override
    public void updateFromFieldAssignment(
            Node lhs, Node rhs, ClassTree classTree, AnnotatedTypeFactory atf) {

        Element element;
        String fieldName;
        if (lhs instanceof FieldAccessNode) {
            element = ((FieldAccessNode) lhs).getElement();
            fieldName = ((FieldAccessNode) lhs).getFieldName();
        } else if (lhs instanceof LocalVariableNode) {
            element = ((LocalVariableNode) lhs).getElement();
            fieldName = ((LocalVariableNode) lhs).getName();
        } else {
            throw new BugInCF(
                    "updateFromFieldAssignment received an unexpected node type: "
                            + lhs.getClass());
        }

        // If the inferred field has a declaration annotation with the
        // @IgnoreInWholeProgramInference meta-annotation, exit this routine.
        if (atf.getDeclAnnotation(element, IgnoreInWholeProgramInference.class) != null
                || atf.getDeclAnnotationWithMetaAnnotation(
                                        element, IgnoreInWholeProgramInference.class)
                                .size()
                        > 0) {
            return;
        }

        ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();

        // do not infer types for code that isn't presented as source
        if (!ElementUtils.isElementFromSourceCode(enclosingClass)) {
            return;
        }

        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath, enclosingClass);

        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        AField field = clazz.fields.getVivify(fieldName);
        field.setTypeMirror(lhsATM.getUnderlyingType());
        // TODO: For a primitive such as long, this is yielding just @GuardedBy rather than
        // @GuardedBy({}).
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());
        storage.updateAnnotationSetInScene(
                field.type, atf, jaifPath, rhsATM, lhsATM, TypeUseLocation.FIELD);
    }

    /**
     * Updates the return type of the method methodTree in the Scene of the class with symbol
     * classSymbol. Also updates the return types of methods that this method overrides, if they are
     * available as source.
     *
     * <p>If the Scene does not contain an annotated return type for the method methodTree, then the
     * type of the value passed to the return expression will be added to the return type of that
     * method in the Scene. If the Scene previously contained an annotated return type for the
     * method methodTree, its new type will be the LUB between the previous type and the type of the
     * value passed to the return expression.
     *
     * @param retNode the node that contains the expression returned
     * @param classSymbol the symbol of the class that contains the method
     * @param methodTree the tree of the method whose return type may be updated
     * @param overriddenMethods the methods that the given method return overrides, each indexed by
     *     the annotated type of the class that defines it
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used to update the method's return type
     */
    @Override
    public void updateFromReturn(
            ReturnNode retNode,
            ClassSymbol classSymbol,
            MethodTree methodTree,
            Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods,
            AnnotatedTypeFactory atf) {

        // do not infer types for code that isn't presented as source
        if (methodTree == null
                || !ElementUtils.isElementFromSourceCode(
                        TreeUtils.elementFromDeclaration(methodTree))) {
            return;
        }

        // See Issue 682
        // https://github.com/typetools/checker-framework/issues/682
        if (classSymbol == null) { // TODO: Handle anonymous classes.
            return;
        }
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = classSymbol.flatname.toString();

        String jaifPath = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, jaifPath, classSymbol);

        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodTree);
        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        method.setFieldsFromMethodElement(methodElt);

        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(methodTree).getReturnType();

        // Type of the expression returned
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(retNode.getTree().getExpression());
        storage.updateAnnotationSetInScene(
                method.returnType, atf, jaifPath, rhsATM, lhsATM, TypeUseLocation.RETURN);

        // Now, update return types of overridden methods based on the implementation we just saw.
        // This inference is similar to the inference procedure for method parameters: both are
        // updated based only on the implementations (in this case) or call-sites (for method
        // parameters) that are available to WPI.
        //
        // An alternative implementation would be to:
        //  * update only the method (not overridden methods)
        //  * when finished, propagate the final result to overridden methods
        //
        for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
                overriddenMethods.entrySet()) {

            AnnotatedDeclaredType superclassDecl = pair.getKey();
            ExecutableElement overriddenMethodElement = pair.getValue();

            // do not infer types for code that isn't presented as source
            if (!ElementUtils.isElementFromSourceCode(overriddenMethodElement)) {
                continue;
            }

            AnnotatedExecutableType overriddenMethod =
                    AnnotatedTypes.asMemberOf(
                            atf.getProcessingEnv().getTypeUtils(),
                            atf,
                            superclassDecl,
                            overriddenMethodElement);

            String superClassName = getEnclosingClassName(overriddenMethodElement);
            String superJaifPath = storage.getJaifPath(superClassName);
            AClass superClazz =
                    storage.getAClass(
                            superClassName,
                            superJaifPath,
                            ((MethodSymbol) overriddenMethodElement).enclClass());
            AMethod overriddenMethodInSuperclass =
                    superClazz.methods.getVivify(
                            JVMNames.getJVMMethodSignature(overriddenMethodElement));
            overriddenMethodInSuperclass.setFieldsFromMethodElement(overriddenMethodElement);
            AnnotatedTypeMirror overriddenMethodReturnType = overriddenMethod.getReturnType();

            storage.updateAnnotationSetInScene(
                    overriddenMethodInSuperclass.returnType,
                    atf,
                    superJaifPath,
                    rhsATM,
                    overriddenMethodReturnType,
                    TypeUseLocation.RETURN);
        }
    }

    /** Write all modified scenes into .jaif files or stub files. */
    @Override
    public void writeResultsToFile(OutputFormat outputFormat, BaseTypeChecker checker) {
        storage.writeScenes(outputFormat, checker);
    }

    /**
     * Returns the "flatname" of the class enclosing {@code localVariableNode}
     *
     * @param localVariableNode the {@link LocalVariableNode}
     * @return the "flatname" of the class enclosing {@code localVariableNode}
     */
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    private @BinaryName String getEnclosingClassName(LocalVariableNode localVariableNode) {
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
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    private @BinaryName String getEnclosingClassName(ExecutableElement executableElement) {
        return ((MethodSymbol) executableElement).enclClass().flatName().toString();
    }

    /**
     * Checks whether a given local variable came from a source file or not.
     *
     * <p>By contrast, {@link ElementUtils#isElementFromByteCode(Element)} returns true if there is
     * a classfile for the given element, whether or not there is also a source file.
     *
     * @param localVariableNode the local variable declaration to check
     * @return true if a source file containing the variable is being compiled
     */
    private boolean isElementFromSourceCode(LocalVariableNode localVariableNode) {
        return ElementUtils.isElementFromSourceCode(localVariableNode.getElement());
    }
}
