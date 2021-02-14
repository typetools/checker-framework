package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.scenelib.ASceneWrapper;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
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
import org.checkerframework.javacutil.TypeAnnotationUtils;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.el.AScene;
import scenelib.annotations.el.ATypeElement;
import scenelib.annotations.util.JVMNames;

/**
 * WholeProgramInferenceScenes is an implementation of {@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInference}.
 *
 * <p>Its file format is .jaif files.
 *
 * <p>It stores annotations using the storage class ({@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesStorage}).
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
@SuppressWarnings("UnusedMethod") // TEMPORARY
public class WholeProgramInferenceScenes implements WholeProgramInference {

    /** The type factory associated with this. */
    protected final AnnotatedTypeFactory atypeFactory;

    /** This stores the inferred annotations. */
    protected final WholeProgramInferenceScenesStorage storage;

    /**
     * Constructs a new {@code WholeProgramInferenceScenes} that has not yet inferred any
     * annotations.
     *
     * @param atypeFactory the associated type factory
     */
    public WholeProgramInferenceScenes(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
        boolean isNullness =
                atypeFactory.getClass().getSimpleName().equals("NullnessAnnotatedTypeFactory");
        boolean ignoreNullAssignments = !isNullness;
        this.storage = new WholeProgramInferenceScenesStorage(atypeFactory, ignoreNullAssignments);
    }

    /**
     * Returns the file corresponding to the given element.
     *
     * @param elt an element
     * @return the path to the file where inference results for the element will be written
     */
    private String getFileForElement(Element elt) {
        String className;
        switch (elt.getKind()) {
            case CONSTRUCTOR:
            case METHOD:
                className = getEnclosingClassName((ExecutableElement) elt);
                break;
            case LOCAL_VARIABLE:
                className = getEnclosingClassName((LocalVariableNode) elt);
                break;
            case FIELD:
                ClassSymbol enclosingClass = ((VarSymbol) elt).enclClass();
                className = enclosingClass.flatname.toString();
                break;
            default:
                throw new BugInCF("What element? %s %s", elt.getKind(), elt);
        }
        String file = storage.getJaifPath(className);
        return file;
    }

    /**
     * Get the annotations for a class.
     *
     * @param className the name of the class, in binary form
     * @param file the path to the file that represents the class
     * @param classSymbol optionally, the ClassSymbol representing the class
     * @return the annotations for the class
     */
    private AClass getClassAnnos(
            @BinaryName String className, String file, @Nullable ClassSymbol classSymbol) {
        return storage.getAClass(className, file, classSymbol);
    }

    /**
     * Get the annotations for a method or constructor.
     *
     * @param methodElt the method or constructor
     * @return the annotations for a method or constructor
     */
    private AMethod getMethodAnnos(ExecutableElement methodElt) {
        String className = getEnclosingClassName(methodElt);
        String file = getFileForElement(methodElt);
        AClass classAnnos = getClassAnnos(className, file, ((MethodSymbol) methodElt).enclClass());
        AMethod methodAnnos =
                classAnnos.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
        methodAnnos.setFieldsFromMethodElement(methodElt);
        return methodAnnos;
    }

    /**
     * Get the annotations for a formal parameter type.
     *
     * @param methodElt the method or constructor
     * @param i the parameter index (0-based)
     * @param paramATM the parameter type
     * @param ve the parameter variable
     * @param atypeFactory the type factory
     * @return the annotations for a formal parameter type
     */
    @SuppressWarnings("UnusedVariable")
    private ATypeElement getParameterType(
            ExecutableElement methodElt,
            int i,
            AnnotatedTypeMirror paramATM,
            VariableElement ve,
            AnnotatedTypeFactory atypeFactory) {
        AMethod methodAnnos = getMethodAnnos(methodElt);
        AField param =
                methodAnnos.vivifyAndAddTypeMirrorToParameter(
                        i, paramATM.getUnderlyingType(), ve.getSimpleName());
        return param.type;
    }

    /**
     * Get the annotations for the receiver type.
     *
     * @param methodElt the method or constructor
     * @param paramATM the receiver type
     * @param atypeFactory the type factory
     * @return the annotations for the receiver type
     */
    @SuppressWarnings("UnusedVariable")
    private ATypeElement getReceiverType(
            ExecutableElement methodElt,
            AnnotatedTypeMirror paramATM,
            AnnotatedTypeFactory atypeFactory) {
        AMethod methodAnnos = getMethodAnnos(methodElt);
        return methodAnnos.receiver.type;
    }

    /**
     * Get the annotations for the return type.
     *
     * @param methodElt the method or constructor
     * @param atm the return type
     * @param atypeFactory the type factory
     * @return the annotations for the return type
     */
    @SuppressWarnings("UnusedVariable")
    private ATypeElement getReturnType(
            ExecutableElement methodElt,
            AnnotatedTypeMirror atm,
            AnnotatedTypeFactory atypeFactory) {
        AMethod methodAnnos = getMethodAnnos(methodElt);
        return methodAnnos.returnType;
    }

    /**
     * Get the annotations for a field type.
     *
     * @param element the element for the field
     * @param fieldName the simple field name
     * @param lhsATM the field type
     * @param atypeFactory the annotated type factory
     * @return the annotations for a field type
     */
    @SuppressWarnings("UnusedVariable")
    private ATypeElement getFieldType(
            Element element,
            String fieldName,
            AnnotatedTypeMirror lhsATM,
            AnnotatedTypeFactory atypeFactory) {
        ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();
        String file = getFileForElement(element);
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        AClass classAnnos = getClassAnnos(className, file, enclosingClass);
        AField field = classAnnos.fields.getVivify(fieldName);
        field.setTypeMirror(lhsATM.getUnderlyingType());
        return field.type;
    }

    /**
     * Returns the pre- or postcondition annotations for a field.
     *
     * @param preOrPost whether to get the precondition or postcondition
     * @param methodElement the method
     * @param fieldElement the field
     * @param atypeFactory the type factory
     * @return the pre- or postcondition annotations for a field
     */
    private ATypeElement getPreOrPostconditionsForField(
            Analysis.BeforeOrAfter preOrPost,
            ExecutableElement methodElement,
            VariableElement fieldElement,
            AnnotatedTypeFactory atypeFactory) {
        switch (preOrPost) {
            case BEFORE:
                return getPreconditionsForField(methodElement, fieldElement, atypeFactory);
            case AFTER:
                return getPostconditionsForField(methodElement, fieldElement, atypeFactory);
            default:
                throw new BugInCF("Unexpected " + preOrPost);
        }
    }

    /**
     * Returns the precondition annotations for a field.
     *
     * @param methodElement the method
     * @param fieldElement the field
     * @param atypeFactory the type factory
     * @return the precondition annotations for a field
     */
    @SuppressWarnings("UnusedVariable")
    private ATypeElement getPreconditionsForField(
            ExecutableElement methodElement,
            VariableElement fieldElement,
            AnnotatedTypeFactory atypeFactory) {
        AMethod methodAnnos = getMethodAnnos(methodElement);
        TypeMirror typeMirror = TypeAnnotationUtils.unannotatedType(fieldElement.asType());
        return methodAnnos.vivifyAndAddTypeMirrorToPrecondition(fieldElement, typeMirror).type;
    }

    /**
     * Returns the postcondition annotations for a field.
     *
     * @param methodElement the method
     * @param fieldElement the field
     * @param atypeFactory the type factory
     * @return the postcondition annotations for a field
     */
    @SuppressWarnings("UnusedVariable")
    private ATypeElement getPostconditionsForField(
            ExecutableElement methodElement,
            VariableElement fieldElement,
            AnnotatedTypeFactory atypeFactory) {
        AMethod methodAnnos = getMethodAnnos(methodElement);
        TypeMirror typeMirror = TypeAnnotationUtils.unannotatedType(fieldElement.asType());
        return methodAnnos.vivifyAndAddTypeMirrorToPostcondition(fieldElement, typeMirror).type;
    }

    @Override
    public void updateFromObjectCreation(
            ObjectCreationNode objectCreationNode,
            ExecutableElement constructorElt,
            CFAbstractStore<?, ?> store) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(constructorElt)) {
            return;
        }

        List<Node> arguments = objectCreationNode.getArguments();
        updateInferredExecutableParameterTypes(constructorElt, arguments);
        updateContracts(Analysis.BeforeOrAfter.BEFORE, constructorElt, store);
    }

    @Override
    public void updateFromMethodInvocation(
            MethodInvocationNode methodInvNode,
            Tree receiverTree,
            ExecutableElement methodElt,
            CFAbstractStore<?, ?> store) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        List<Node> arguments = methodInvNode.getArguments();
        updateInferredExecutableParameterTypes(methodElt, arguments);
        updateContracts(Analysis.BeforeOrAfter.BEFORE, methodElt, store);
    }

    /**
     * Updates inferred parameter types based on a call to a method or constructor.
     *
     * @param methodElt the element of the method or constructor being invoked
     * @param arguments the arguments of the invocation
     */
    private void updateInferredExecutableParameterTypes(
            ExecutableElement methodElt, List<Node> arguments) {

        String file = getFileForElement(methodElt);

        for (int i = 0; i < arguments.size(); i++) {
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

            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);
            AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(argTree);
            atypeFactory.wpiAdjustForUpdateNonField(argATM);
            ATypeElement paramType = getParameterType(methodElt, i, paramATM, ve, atypeFactory);
            updateAnnotationSet(paramType, TypeUseLocation.PARAMETER, argATM, paramATM, file);
        }
    }

    @Override
    public void updateContracts(
            Analysis.BeforeOrAfter preOrPost,
            ExecutableElement methodElt,
            CFAbstractStore<?, ?> store) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        if (store == null) {
            throw new BugInCF(
                    "updateContracts(%s, %s, null) for %s",
                    preOrPost, methodElt, atypeFactory.getClass().getSimpleName());
        }

        // TODO: Probably move some part of this into the AnnotatedTypeFactory.

        // This code only handles fields of "this", for now.  In the future, extend it to other
        // expressions.
        TypeElement containingClass = (TypeElement) methodElt.getEnclosingElement();
        ThisReference thisReference = new ThisReference(containingClass.asType());
        ClassName classNameReceiver = new ClassName(containingClass.asType());
        for (VariableElement fieldElement :
                ElementFilter.fieldsIn(containingClass.getEnclosedElements())) {
            FieldAccess fa =
                    new FieldAccess(
                            (ElementUtils.isStatic(fieldElement)
                                    ? classNameReceiver
                                    : thisReference),
                            fieldElement.asType(),
                            fieldElement);
            CFAbstractValue<?> v = store.getFieldValue(fa);
            AnnotatedTypeMirror fieldDeclType = atypeFactory.getAnnotatedType(fieldElement);
            AnnotatedTypeMirror inferredType;
            if (v != null) {
                // This field is in the store.
                inferredType = convertCFAbstractValueToAnnotatedTypeMirror(v, fieldDeclType);
                atypeFactory.wpiAdjustForUpdateNonField(inferredType);
            } else {
                // This field is not in the store. Add its declared type.
                inferredType = atypeFactory.getAnnotatedType(fieldElement);
            }

            ATypeElement preOrPostConditionAnnos =
                    getPreOrPostconditionsForField(
                            preOrPost, methodElt, fieldElement, atypeFactory);

            String file = getFileForElement(methodElt);
            updateAnnotationSet(
                    preOrPostConditionAnnos,
                    TypeUseLocation.FIELD,
                    inferredType,
                    fieldDeclType,
                    file,
                    false);
        }
    }

    /**
     * Converts a CFAbstractValue to an AnnotatedTypeMirror.
     *
     * @param v a value to convert to an AnnotatedTypeMirror
     * @param fieldType a type that is copied, then updated to use {@code v}'s annotations
     * @return a copy of {@code fieldType} with {@code v}'s annotations
     */
    private AnnotatedTypeMirror convertCFAbstractValueToAnnotatedTypeMirror(
            CFAbstractValue<?> v, AnnotatedTypeMirror fieldType) {
        AnnotatedTypeMirror result = fieldType.deepCopy();
        result.replaceAnnotations(v.getAnnotations());
        return result;
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

        String file = getFileForElement(methodElt);

        for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);
            AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
            atypeFactory.wpiAdjustForUpdateNonField(argATM);
            ATypeElement paramType = getParameterType(methodElt, i, paramATM, ve, atypeFactory);
            updateAnnotationSet(paramType, TypeUseLocation.PARAMETER, argATM, paramATM, file);
        }

        AnnotatedDeclaredType argADT = overriddenMethod.getReceiverType();
        if (argADT != null) {
            AnnotatedTypeMirror paramATM =
                    atypeFactory.getAnnotatedType(methodTree).getReceiverType();
            if (paramATM != null) {
                ATypeElement receiver = getReceiverType(methodElt, paramATM, atypeFactory);
                updateAnnotationSet(receiver, TypeUseLocation.RECEIVER, argADT, paramATM, file);
            }
        }
    }

    @Override
    public void updateFromFormalParameterAssignment(
            LocalVariableNode lhs, Node rhs, VariableElement paramElt) {
        // Don't infer types for code that isn't presented as source.
        if (!isElementFromSourceCode(lhs)) {
            return;
        }

        Tree rhsTree = rhs.getTree();
        if (rhsTree == null) {
            // TODO: Handle variable-length list as parameter.
            // An ArrayCreationNode with a null tree is created when the
            // parameter is a variable-length list. We are ignoring it for now.
            // See Issue 682
            // https://github.com/typetools/checker-framework/issues/682
            return;
        }

        ExecutableElement methodElt = (ExecutableElement) paramElt.getEnclosingElement();

        AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(paramElt);
        AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(rhsTree);
        atypeFactory.wpiAdjustForUpdateNonField(argATM);
        int i = methodElt.getParameters().indexOf(paramElt);
        assert i != -1;
        ATypeElement paramType = getParameterType(methodElt, i, paramATM, paramElt, atypeFactory);
        String file = getFileForElement(methodElt);
        updateAnnotationSet(paramType, TypeUseLocation.PARAMETER, argATM, paramATM, file);
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
        atypeFactory.wpiAdjustForUpdateField(lhs.getTree(), element, fieldName, rhsATM);

        updateFieldFromType(lhs.getTree(), element, fieldName, rhsATM);
    }

    @Override
    public void updateFieldFromType(
            Tree lhsTree, Element element, String fieldName, AnnotatedTypeMirror rhsATM) {

        if (ignoreFieldInWPI(element, fieldName)) {
            return;
        }

        ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();

        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(enclosingClass)) {
            return;
        }

        String file = getFileForElement(element);

        AnnotatedTypeMirror lhsATM = atypeFactory.getAnnotatedType(lhsTree);
        ATypeElement fieldType = getFieldType(element, fieldName, lhsATM, atypeFactory);

        updateAnnotationSet(fieldType, TypeUseLocation.FIELD, rhsATM, lhsATM, file);
    }

    /**
     * Returns true if an assignment to the given field should be ignored by WPI.
     *
     * @param element the field's element
     * @param fieldName the field's name
     * @return true if an assignment to the given field should be ignored by WPI
     */
    protected boolean ignoreFieldInWPI(Element element, String fieldName) {
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

        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(enclosingClass)) {
            return true;
        }

        return false;
    }

    /**
     * Updates the return type of the method methodDeclTree in the Scene of the class with symbol
     * classSymbol. Also updates the return types of methods that this method overrides, if they are
     * available as source.
     *
     * <p>If the Scene does not contain an annotated return type for the method methodDeclTree, then
     * the type of the value passed to the return expression will be added to the return type of
     * that method in the Scene. If the Scene previously contained an annotated return type for the
     * method methodDeclTree, its new type will be the LUB between the previous type and the type of
     * the value passed to the return expression.
     *
     * @param retNode the node that contains the expression returned
     * @param classSymbol the symbol of the class that contains the method
     * @param methodDeclTree the declaration of the method whose return type may be updated
     * @param overriddenMethods the methods that the given method return overrides, each indexed by
     *     the annotated type of the class that defines it
     */
    @Override
    public void updateFromReturn(
            ReturnNode retNode,
            ClassSymbol classSymbol,
            MethodTree methodDeclTree,
            Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods) {
        // Don't infer types for code that isn't presented as source.
        if (methodDeclTree == null
                || !ElementUtils.isElementFromSourceCode(
                        TreeUtils.elementFromDeclaration(methodDeclTree))) {
            return;
        }

        // Whole-program inference ignores some locations.  See Issue 682:
        // https://github.com/typetools/checker-framework/issues/682
        if (classSymbol == null) { // TODO: Handle anonymous classes.
            return;
        }

        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodDeclTree);
        String file = getFileForElement(methodElt);

        AnnotatedTypeMirror lhsATM = atypeFactory.getAnnotatedType(methodDeclTree).getReturnType();

        // Type of the expression returned
        AnnotatedTypeMirror rhsATM =
                atypeFactory.getAnnotatedType(retNode.getTree().getExpression());
        atypeFactory.wpiAdjustForUpdateNonField(rhsATM);
        DependentTypesHelper dependentTypesHelper =
                ((GenericAnnotatedTypeFactory) atypeFactory).getDependentTypesHelper();
        dependentTypesHelper.delocalize(methodDeclTree, rhsATM);
        ATypeElement returnTypeAnnos = getReturnType(methodElt, lhsATM, atypeFactory);
        updateAnnotationSet(returnTypeAnnos, TypeUseLocation.RETURN, rhsATM, lhsATM, file);

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

            String superClassFile = getFileForElement(overriddenMethodElement);
            AnnotatedTypeMirror overriddenMethodReturnType = overriddenMethod.getReturnType();
            ATypeElement storedOverriddenMethodReturnType =
                    getReturnType(
                            overriddenMethodElement, overriddenMethodReturnType, atypeFactory);

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

        AMethod methodAnnos = getMethodAnnos(methodElt);

        scenelib.annotations.Annotation sceneAnno =
                AnnotationConverter.annotationMirrorToAnnotation(anno);
        methodAnnos.tlAnnotationsHere.add(sceneAnno);
    }

    /**
     * Updates the set of annotations in a location in a program.
     *
     * @param typeToUpdate the type whose annotations are modified by this method
     * @param defLoc the location where the annotation will be added
     * @param rhsATM the RHS of the annotated type on the source code
     * @param lhsATM the LHS of the annotated type on the source code
     * @param file path to the annotation file containing the executable; used for marking the scene
     *     as modified (needing to be written to disk)
     */
    protected final void updateAnnotationSet(
            ATypeElement typeToUpdate,
            TypeUseLocation defLoc,
            AnnotatedTypeMirror rhsATM,
            AnnotatedTypeMirror lhsATM,
            String file) {
        updateAnnotationSet(typeToUpdate, defLoc, rhsATM, lhsATM, file, true);
    }

    /**
     * Updates the set of annotations in a location in a program.
     *
     * <ul>
     *   <li>If there was no previous annotation for that location, then the updated set will be the
     *       annotations in rhsATM.
     *   <li>If there was a previous annotation, the updated set will be the LUB between the
     *       previous annotation and rhsATM.
     * </ul>
     *
     * <p>Subclasses can customize its behavior.
     *
     * @param typeToUpdate the type whose annotations are modified by this method
     * @param defLoc the location where the annotation will be added
     * @param rhsATM the RHS of the annotated type on the source code
     * @param lhsATM the LHS of the annotated type on the source code
     * @param file path to the annotation file containing the executable; used for marking the scene
     *     as modified (needing to be written to disk)
     * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
     *     source code
     */
    protected void updateAnnotationSet(
            ATypeElement typeToUpdate,
            TypeUseLocation defLoc,
            AnnotatedTypeMirror rhsATM,
            AnnotatedTypeMirror lhsATM,
            String file,
            boolean ignoreIfAnnotated) {
        storage.updateAnnotationSetInScene(
                typeToUpdate, defLoc, rhsATM, lhsATM, file, ignoreIfAnnotated);
    }

    ///
    /// Classes and class names
    ///

    /**
     * Returns the binary name of the type declaration in {@code element}
     *
     * @param element a type declaration
     * @return the binary name of {@code element}
     */
    @SuppressWarnings({
        "signature", // https://tinyurl.com/cfissue/3094
        "UnusedMethod"
    })
    private @BinaryName String getClassName(Element element) {
        return ((ClassSymbol) element).flatName().toString();
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
     * Returns the "flatname" of the class enclosing {@code variableElement}
     *
     * @param variableElement the VariableElement
     * @return the "flatname" of the class enclosing {@code variableElement}
     */
    private @BinaryName String getEnclosingClassName(VariableElement variableElement) {
        return getClassName(ElementUtils.enclosingTypeElement(variableElement));
    }

    /**
     * Returns the "flatname" of the class enclosing {@code localVariableNode}.
     *
     * @param localVariableNode the {@link LocalVariableNode}
     * @return the "flatname" of the class enclosing {@code localVariableNode}
     */
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    private @BinaryName String getEnclosingClassName(LocalVariableNode localVariableNode) {
        return ((ClassSymbol) ElementUtils.enclosingTypeElement(localVariableNode.getElement()))
                .flatName()
                .toString();
    }

    /**
     * Returns the top-level class that contains {@code element}.
     *
     * @param element the element wose enclosing class to find
     * @return an element for a class containing {@code element} that isn't contained in another
     *     class
     */
    private TypeElement toplevelEnclosingClass(Element element) {
        if (ElementUtils.enclosingTypeElement(element) == null) {
            return (TypeElement) element;
        }

        TypeElement result = ElementUtils.enclosingTypeElement(element);
        TypeElement enclosing = ElementUtils.enclosingTypeElement(result);
        // ElementUtils.enclosingType returns its argument if it's already a class, so an Element
        // being the same as its enclosing class is a sufficient stopping condition.
        while (enclosing != null && !enclosing.equals(result)) {
            result = ElementUtils.enclosingTypeElement(result);
            enclosing = ElementUtils.enclosingTypeElement(result);
        }

        return result;
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

    /**
     * Obtain the type from an ATypeElement (which is part of a Scene).
     *
     * @param typeMirror the underlying type for the result
     * @param type the ATypeElement from which to obtain annotations
     * @return an annotated type mirror with underlying type {@code typeMirror} and annotations from
     *     {@code type}
     */
    public AnnotatedTypeMirror atmFromATypeElement(TypeMirror typeMirror, ATypeElement type) {
        return storage.atmFromATypeElement(typeMirror, type);
    }

    ///
    /// Writing to a file
    ///

    // The prepare*ForWriting hooks are needed in addition to the postProcessClassTree hook because
    // a scene may be modifed and written at any time, including before or after
    // postProcessClassTree is called.

    /**
     * Side-effects the compilation unit annotations to make any desired changes before writing to a
     * file.
     *
     * @param compilationUnitAnnos the compilation unit annotations to modify
     */
    public void prepareSceneForWriting(AScene compilationUnitAnnos) {
        for (Map.Entry<String, AClass> classEntry : compilationUnitAnnos.classes.entrySet()) {
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
        atypeFactory.prepareMethodForWriting(methodAnnos);
    }

    @Override
    public void writeResultsToFile(OutputFormat outputFormat, BaseTypeChecker checker) {
        for (String file : storage.modifiedScenes) {
            ASceneWrapper scene = storage.scenes.get(file);
            prepareSceneForWriting(scene.getAScene());
        }

        storage.writeScenes(outputFormat, checker);
    }

    ///
    /// Storing annotations
    ///

    // That is done in WholeProgramInferenceScenesStorage.java

}
