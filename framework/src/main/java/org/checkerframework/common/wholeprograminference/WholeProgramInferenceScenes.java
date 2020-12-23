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
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.scenelib.ASceneWrapper;
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
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.el.AScene;
import scenelib.annotations.el.ATypeElement;
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
    protected final WholeProgramInferenceScenesStorage storage;

    /** The type factory associated with this WholeProgramInferenceScenes. */
    protected final AnnotatedTypeFactory atypeFactory;

    /**
     * Constructs a new {@code WholeProgramInferenceScenes} that has not yet inferred any
     * annotations.
     *
     * @param atypeFactory the associated type factory
     */
    public WholeProgramInferenceScenes(AnnotatedTypeFactory atypeFactory) {
        this(atypeFactory, true);
    }

    /**
     * Constructs a new {@code WholeProgramInferenceScenes} that has not yet inferred any
     * annotations.
     *
     * @param atypeFactory the associated type factory
     * @param ignoreNullAssignments indicates whether assignments where the rhs is null should be
     *     ignored
     */
    public WholeProgramInferenceScenes(
            AnnotatedTypeFactory atypeFactory, boolean ignoreNullAssignments) {
        this.atypeFactory = atypeFactory;
        storage = new WholeProgramInferenceScenesStorage(atypeFactory, ignoreNullAssignments);
    }

    @Override
    public void updateFromObjectCreation(
            ObjectCreationNode objectCreationNode, ExecutableElement constructorElt) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(constructorElt)) {
            return;
        }

        String className = getEnclosingClassName(constructorElt);
        String file = storage.getJaifPath(className);
        AClass clazz =
                storage.getAClass(className, file, ((MethodSymbol) constructorElt).enclClass());
        AMethod constructorAnnos =
                clazz.methods.getVivify(JVMNames.getJVMMethodSignature(constructorElt));
        constructorAnnos.setFieldsFromMethodElement(constructorElt);

        List<Node> arguments = objectCreationNode.getArguments();
        updateInferredExecutableParameterTypes(constructorElt, file, constructorAnnos, arguments);
    }

    @Override
    public void updateFromMethodInvocation(
            MethodInvocationNode methodInvNode, Tree receiverTree, ExecutableElement methodElt) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String className = getEnclosingClassName(methodElt);
        String file = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, file, ((MethodSymbol) methodElt).enclClass());
        AMethod methodAnnos = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        methodAnnos.setFieldsFromMethodElement(methodElt);

        List<Node> arguments = methodInvNode.getArguments();
        updateInferredExecutableParameterTypes(methodElt, file, methodAnnos, arguments);
    }

    /**
     * Updates inferred parameter types based on a call to a method or constructor.
     *
     * @param methodElt the element of the method or constructor being invoked
     * @param file the annotation file containing the executable; used for marking the class as
     *     modified (needing to be written to disk)
     * @param executableAnnos the representation of the executable's annotations
     * @param arguments the arguments of the invocation
     */
    private void updateInferredExecutableParameterTypes(
            ExecutableElement methodElt,
            String file,
            AMethod executableAnnos,
            List<Node> arguments) {

        for (int i = 0; i < arguments.size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);

            Node arg = arguments.get(i);
            Tree argTree = arg.getTree();
            if (argTree == null) {
                // TODO: Handle variable-length list as parameter.
                // An ArrayCreationNode with a null tree is created when the
                // parameter is a variable-length list. We are ignoring it for now.
                // See Issue 682
                // https://github.com/typetools/checker-framework/issues/682
                continue;
            }
            AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(argTree);
            AField param =
                    executableAnnos.vivifyAndAddTypeMirrorToParameter(
                            i, argATM.getUnderlyingType(), ve.getSimpleName());
            updateAnnotationSet(param.type, TypeUseLocation.PARAMETER, argATM, paramATM, file);
        }
    }

    @Override
    public void updateFromOverride(
            MethodTree methodTree,
            ExecutableElement methodElt,
            AnnotatedExecutableType overriddenMethod) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String className = getEnclosingClassName(methodElt);
        String file = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, file, ((MethodSymbol) methodElt).enclClass());
        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        method.setFieldsFromMethodElement(methodElt);

        for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);

            AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
            AField param =
                    method.vivifyAndAddTypeMirrorToParameter(
                            i, argATM.getUnderlyingType(), ve.getSimpleName());
            updateAnnotationSet(param.type, TypeUseLocation.PARAMETER, argATM, paramATM, file);
        }

        AnnotatedDeclaredType argADT = overriddenMethod.getReceiverType();
        if (argADT != null) {
            AnnotatedTypeMirror paramATM =
                    atypeFactory.getAnnotatedType(methodTree).getReceiverType();
            if (paramATM != null) {
                AField receiver = method.receiver;
                updateAnnotationSet(
                        receiver.type, TypeUseLocation.RECEIVER, argADT, paramATM, file);
            }
        }
    }

    @Override
    public void updateFromLocalAssignment(
            LocalVariableNode lhs, Node rhs, ClassTree classTree, MethodTree methodTree) {
        // Don't infer types for code that isn't presented as source.
        if (!isElementFromSourceCode(lhs)) {
            return;
        }

        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodTree);
        String className = getEnclosingClassName(lhs);
        String file = storage.getJaifPath(className);
        AClass clazz =
                storage.getAClass(
                        className, file, (ClassSymbol) TreeUtils.elementFromDeclaration(classTree));
        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        method.setFieldsFromMethodElement(methodElt);

        List<? extends VariableTree> params = methodTree.getParameters();
        // Look-up parameter by name:
        for (int i = 0; i < params.size(); i++) {
            VariableTree vt = params.get(i);
            if (vt.getName().contentEquals(lhs.getName())) {
                Tree rhsTree = rhs.getTree();
                if (rhsTree == null) {
                    // TODO: Handle variable-length list as parameter.
                    // An ArrayCreationNode with a null tree is created when the
                    // parameter is a variable-length list. We are ignoring it for now.
                    // See Issue 682
                    // https://github.com/typetools/checker-framework/issues/682
                    continue;
                }
                AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(vt);
                AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(rhsTree);
                VariableElement ve = TreeUtils.elementFromDeclaration(vt);
                AField param =
                        method.vivifyAndAddTypeMirrorToParameter(
                                i, argATM.getUnderlyingType(), ve.getSimpleName());

                updateAnnotationSet(param.type, TypeUseLocation.PARAMETER, argATM, paramATM, file);
                break;
            }
        }
    }

    @Override
    public void updateFromFieldAssignment(Node lhs, Node rhs, ClassTree classTree) {

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

        // TODO: For a primitive such as long, this is yielding just @GuardedBy rather than
        // @GuardedBy({}).
        AnnotatedTypeMirror rhsATM = atypeFactory.getAnnotatedType(rhs.getTree());

        updateFieldFromType(lhs.getTree(), element, fieldName, rhsATM);
    }

    @Override
    public void updateFieldFromType(
            Tree lhsTree, Element element, String fieldName, AnnotatedTypeMirror rhsATM) {

        if (ignoreFieldInWPI(element, fieldName)) {
            return;
        }

        ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();

        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        String file = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, file, enclosingClass);

        AnnotatedTypeMirror lhsATM = atypeFactory.getAnnotatedType(lhsTree);
        AField field = clazz.fields.getVivify(fieldName);
        field.setTypeMirror(lhsATM.getUnderlyingType());

        updateAnnotationSet(field.type, TypeUseLocation.FIELD, rhsATM, lhsATM, file);
    }

    /**
     * Returns true if an assignment to the given field should be ignored by WPI.
     *
     * @param element the field's element
     * @param fieldName the field's name
     * @return true if an assignment to the given field should be ignored by WPI
     */
    private boolean ignoreFieldInWPI(Element element, String fieldName) {
        // Do not attempt to infer types for fields that do not have valid
        // names. For example, compiler-generated temporary variables will
        // have invalid names. Recording facts about fields with
        // invalid names causes jaif-based WPI to crash when reading the .jaif
        // file, and stub-based WPI to generate unparseable stub files.
        // See https://github.com/typetools/checker-framework/issues/3442
        if (!SourceVersion.isIdentifier(fieldName)) {
            return true;
        }

        // If the inferred field has a declaration annotation with the
        // @IgnoreInWholeProgramInference meta-annotation, exit this routine.
        if (atypeFactory.getDeclAnnotation(element, IgnoreInWholeProgramInference.class) != null
                || atypeFactory
                                .getDeclAnnotationWithMetaAnnotation(
                                        element, IgnoreInWholeProgramInference.class)
                                .size()
                        > 0) {
            return true;
        }

        ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();

        // do not infer types for code that isn't presented as source
        if (!ElementUtils.isElementFromSourceCode(enclosingClass)) {
            return true;
        }

        return false;
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
     */
    @Override
    public void updateFromReturn(
            ReturnNode retNode,
            ClassSymbol classSymbol,
            MethodTree methodTree,
            Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods) {
        // Don't infer types for code that isn't presented as source.
        if (methodTree == null
                || !ElementUtils.isElementFromSourceCode(
                        TreeUtils.elementFromDeclaration(methodTree))) {
            return;
        }

        // Whole-program inference ignores some locations.  See Issue 682:
        // https://github.com/typetools/checker-framework/issues/682
        if (classSymbol == null) { // TODO: Handle anonymous classes.
            return;
        }

        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodTree);
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = classSymbol.flatname.toString();
        String file = storage.getJaifPath(className);
        AClass clazz = storage.getAClass(className, file, classSymbol);

        AMethod method = clazz.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        method.setFieldsFromMethodElement(methodElt);

        AnnotatedTypeMirror lhsATM = atypeFactory.getAnnotatedType(methodTree).getReturnType();

        // Type of the expression returned
        AnnotatedTypeMirror rhsATM =
                atypeFactory.getAnnotatedType(retNode.getTree().getExpression());
        DependentTypesHelper dependentTypesHelper =
                ((GenericAnnotatedTypeFactory) atypeFactory).getDependentTypesHelper();
        if (dependentTypesHelper != null) {
            dependentTypesHelper.standardizeReturnType(
                    methodTree, rhsATM, /*removeErroneousExpressions=*/ true);
        }
        updateAnnotationSet(method.returnType, TypeUseLocation.RETURN, rhsATM, lhsATM, file);

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

            // Don't infer types for code that isn't presented as source.
            if (!ElementUtils.isElementFromSourceCode(overriddenMethodElement)) {
                continue;
            }

            AnnotatedExecutableType overriddenMethod =
                    AnnotatedTypes.asMemberOf(
                            atypeFactory.getProcessingEnv().getTypeUtils(),
                            atypeFactory,
                            superclassDecl,
                            overriddenMethodElement);

            String superClassName = getEnclosingClassName(overriddenMethodElement);
            String superClassFile = storage.getJaifPath(superClassName);
            AClass superClassAnnos =
                    storage.getAClass(
                            superClassName,
                            superClassFile,
                            ((MethodSymbol) overriddenMethodElement).enclClass());
            AMethod overriddenMethodInSuperclass =
                    superClassAnnos.methods.getVivify(
                            JVMNames.getJVMMethodSignature(overriddenMethodElement));
            overriddenMethodInSuperclass.setFieldsFromMethodElement(overriddenMethodElement);
            AnnotatedTypeMirror overriddenMethodReturnType = overriddenMethod.getReturnType();
            ATypeElement storedOverriddenMethodReturnType = overriddenMethodInSuperclass.returnType;

            updateAnnotationSet(
                    storedOverriddenMethodReturnType,
                    TypeUseLocation.RETURN,
                    rhsATM,
                    overriddenMethodReturnType,
                    superClassFile);
        }
    }

    @Override
    public void addMethodDeclarationAnnotation(ExecutableElement methodElt, AnnotationMirror anno) {

        // Do not infer types for library code, only for type-checked source code.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String className = getEnclosingClassName(methodElt);
        String file = storage.getJaifPath(className);
        AClass classAnnos =
                storage.getAClass(className, file, ((MethodSymbol) methodElt).enclClass());
        AMethod method = classAnnos.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));

        scenelib.annotations.Annotation sceneAnno =
                AnnotationConverter.annotationMirrorToAnnotation(anno);
        method.tlAnnotationsHere.add(sceneAnno);
    }

    /**
     * Updates the set of annotations in a location in a program.
     *
     * <p>Calls {@link WholeProgramInferenceScenesStorage#updateAnnotationSetInScene}, forwarding
     * the arguments.
     *
     * <p>Subclasses can customize its behavior.
     *
     * @param typeToUpdate the type whose annotations are modified by this method
     * @param defLoc the location where the annotation will be added
     * @param rhsATM the RHS of the annotated type on the source code
     * @param lhsATM the LHS of the annotated type on the source code
     * @param file path to the annotation file containing the executable; used for marking the scene
     *     as modified (needing to be written to disk)
     */
    protected void updateAnnotationSet(
            ATypeElement typeToUpdate,
            TypeUseLocation defLoc,
            AnnotatedTypeMirror rhsATM,
            AnnotatedTypeMirror lhsATM,
            String file) {
        storage.updateAnnotationSetInScene(typeToUpdate, defLoc, rhsATM, lhsATM, file);
    }

    ///
    /// Classes and class names
    ///

    /**
     * Returns the "flatname" of the class enclosing {@code localVariableNode}.
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
     * Returns the "flatname" of the class enclosing {@code executableElement}.
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

    ///
    /// Writing to a file
    ///

    // The prepare*ForWriting hooks are needed in addition to the postProcessClassTree hook because
    // a scene may be modifed and written at any time, including before or after
    // postProcessClassTree is called.

    /**
     * Side-effects the AScene to make any desired changes before writing to a file.
     *
     * @param scene the AScene to modify
     */
    public void prepareSceneForWriting(AScene scene) {
        for (Map.Entry<String, AClass> classEntry : scene.classes.entrySet()) {
            prepareClassForWriting(classEntry.getValue());
        }
    }

    /**
     * Side-effects the class annotations to make any desired changes before writing to a file.
     *
     * @param classAnnos the class annotations to modify
     */
    public void prepareClassForWriting(AClass classAnnos) {
        for (Map.Entry<String, AMethod> methodEntry : classAnnos.methods.entrySet()) {
            prepareMethodForWriting(methodEntry.getValue());
        }
    }

    /**
     * Side-effects the method or constructor annotations to make any desired changes before writing
     * to a file.
     *
     * @param methodAnnos the method or constructor annotations to modify
     */
    public void prepareMethodForWriting(AMethod methodAnnos) {
        // This implementation does nothing.
    }

    @Override
    public void writeResultsToFile(OutputFormat outputFormat, BaseTypeChecker checker) {
        if (outputFormat == OutputFormat.AJAVA) {
            throw new BugInCF("WholeProgramInferenceScenes used with format " + outputFormat);
        }

        for (String file : storage.modifiedScenes) {
            ASceneWrapper scene = storage.scenes.get(file);
            prepareSceneForWriting(scene.getAScene());
        }

        storage.writeScenes(outputFormat, checker);
    }
}
