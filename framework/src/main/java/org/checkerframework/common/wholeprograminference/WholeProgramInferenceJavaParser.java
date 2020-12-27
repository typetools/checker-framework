package org.checkerframework.common.wholeprograminference;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
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
import org.checkerframework.framework.ajava.AjavaUtils;
import org.checkerframework.framework.ajava.AnnotationConversion;
import org.checkerframework.framework.ajava.AnnotationTransferVisitor;
import org.checkerframework.framework.ajava.ClearAnnotationsVisitor;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.ajava.JointJavacJavaParserVisitor;
import org.checkerframework.framework.ajava.StringLiteralConcatenateVisitor;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import scenelib.annotations.util.JVMNames;

/**
 * WholeProgramInferenceJavaParser is an implementation of {@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInference} that reads in the files
 * it processes using JavaParser and stores annotations directly with the JavaParser nodes they
 * apply to. As a result, it only outputs to ajava files. This class behaves identically to {@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes}. See that class
 * for documentation on behavior.
 */
public class WholeProgramInferenceJavaParser implements WholeProgramInference {

    /**
     * Directory where .ajava files will be written to and read from. This directory is relative to
     * where the javac command is executed.
     */
    public static final String AJAVA_FILES_PATH =
            "build" + File.separator + "whole-program-inference" + File.separator;

    // TODO JWAATAJA: What is a "source file added for inference"?
    /**
     * Maps from binary class name to the wrapper containing the class. Contains all classes in
     * source files added for inference.
     */
    private Map<@BinaryName String, ClassOrInterfaceAnnos> classToAnnos;

    /**
     * Files containing classes for which an annotation has been inferred since the last time files
     * were written to disk.
     */
    private Set<String> modifiedFiles;

    /** Mapping from source file to the wrapper for the compilation unit parsed from that file. */
    private Map<String, CompilationUnitAnnos> sourceToAnnos;

    /** For calling declarationFromElement. */
    private AnnotatedTypeFactory atypeFactory;

    /** Indicates whether assignments where the rhs is null should be ignored. */
    private final boolean ignoreNullAssignments;

    /**
     * Constructs a new {@code WholeProgramInferenceJavaParser} that has not yet inferred any
     * annotations.
     *
     * @param atypeFactory the associated type factory
     */
    public WholeProgramInferenceJavaParser(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
        classToAnnos = new HashMap<>();
        modifiedFiles = new HashSet<>();
        sourceToAnnos = new HashMap<>();
        this.ignoreNullAssignments =
                !atypeFactory.getClass().getSimpleName().equals("NullnessAnnotatedTypeFactory");
    }

    /**
     * Returns the file corresponding to the given element.
     *
     * @param elt an element
     * @return the path to the file where inference results for the element will be written
     */
    private String getFileForElement(Element elt) {
        return addClassesForElement(elt);
    }

    /**
     * Get the annotations for a class.
     *
     * @param className the name of the class to get, in binary form
     * @param file the path to the file that represents the class
     * @param classSymbol optionally, the ClassSymbol representing the class
     * @return the annotations for the class
     */
    @SuppressWarnings("UnusedVariable")
    private ClassOrInterfaceAnnos getClassAnnos(
            @BinaryName String className, String file, @Nullable ClassSymbol classSymbol) {
        return classToAnnos.get(className);
    }

    /**
     * Get the annotations for a method or constructor.
     *
     * @param methodElt the method or constructor
     * @param file the annotation file containing the method or constructor
     * @return the annotations for a method or constructor
     */
    @SuppressWarnings("UnusedVariable")
    private CallableDeclarationAnnos getMethodAnnos(ExecutableElement methodElt, String file) {
        String className = getEnclosingClassName(methodElt);
        ClassOrInterfaceAnnos classAnnos =
                getClassAnnos(className, file, ((MethodSymbol) methodElt).enclClass());
        CallableDeclarationAnnos methodAnnos =
                classAnnos.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        return methodAnnos;
    }

    /**
     * Get the annotations for a formal parameter type.
     *
     * @param methodAnnos the method or constructor annotations
     * @param i the parameter index (0-based)
     * @param paramATM the parameter type
     * @param ve the parameter variable
     * @param atypeFactory the type factory
     * @return the annotations for a formal parameter type
     */
    @SuppressWarnings("UnusedVariable")
    private AnnotatedTypeMirror getParameterType(
            CallableDeclarationAnnos methodAnnos,
            int i,
            AnnotatedTypeMirror paramATM,
            VariableElement ve,
            AnnotatedTypeFactory atypeFactory) {
        return methodAnnos.getParameterType(paramATM, i, atypeFactory);
    }

    /**
     * Get the annotations for the receiver type.
     *
     * @param methodAnnos the method or constructor annotations
     * @param paramATM the receiver type
     * @param atypeFactory the type factory
     * @return the annotations for the receiver type
     */
    private AnnotatedTypeMirror getReceiverType(
            CallableDeclarationAnnos methodAnnos,
            AnnotatedTypeMirror paramATM,
            AnnotatedTypeFactory atypeFactory) {
        return methodAnnos.getReceiverType(paramATM, atypeFactory);
    }

    /**
     * Get the annotations for the return type.
     *
     * @param methodAnnos the method or constructor annotations
     * @param atm the return type
     * @param atypeFactory the type factory
     * @return the annotations for the return type
     */
    private AnnotatedTypeMirror getReturnType(
            CallableDeclarationAnnos methodAnnos,
            AnnotatedTypeMirror atm,
            AnnotatedTypeFactory atypeFactory) {
        return methodAnnos.getReturnType(atm, atypeFactory);
    }

    /**
     * Get the annotations for a field type.
     *
     * @param classAnnos the class annotations
     * @param fieldName the simple field name
     * @param lhsATM the field type
     * @param atypeFactory the annotated type factory
     * @return the annotations for a field type
     */
    public AnnotatedTypeMirror getFieldType(
            ClassOrInterfaceAnnos classAnnos,
            String fieldName,
            AnnotatedTypeMirror lhsATM,
            AnnotatedTypeFactory atypeFactory) {
        return classAnnos.fields.get(fieldName).getType(lhsATM, atypeFactory);
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

        String file = getFileForElement(constructorElt);
        CallableDeclarationAnnos constructorAnnos = getMethodAnnos(constructorElt, file);
        List<Node> arguments = objectCreationNode.getArguments();
        updateInferredExecutableParameterTypes(constructorElt, file, constructorAnnos, arguments);
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

        String file = getFileForElement(methodElt);
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt, file);

        // TODO JWAATAJA: Under what circumstances is `methodAnnos` null?
        // TODO JWAATAJA: Why is "valueOf" treated specially?
        // TODO JWAATAJA: This treats all methods named "valueOf" the same, regardless of their
        // signature.
        if (methodAnnos == null) {
            return;
        }

        List<Node> arguments = methodInvNode.getArguments();
        updateInferredExecutableParameterTypes(methodElt, file, methodAnnos, arguments);
        updateContracts(Analysis.BeforeOrAfter.BEFORE, methodElt, store);
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
            CallableDeclarationAnnos executableAnnos,
            List<Node> arguments) {

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
            AnnotatedTypeMirror paramType =
                    getParameterType(executableAnnos, i, paramATM, ve, atypeFactory);
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

        String className = getEnclosingClassName(methodElt);
        String file = addClassesForElement(methodElt);
        ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);

        CallableDeclarationAnnos methodAnnos =
                classAnnos.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        // This will occur when methodAnnos is a synthetic default constructor.
        if (methodAnnos == null) {
            return;
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
            AnnotatedTypeMirror preOrPostConditionAnnos;
            switch (preOrPost) {
                case BEFORE:
                    preOrPostConditionAnnos =
                            methodAnnos.getPreconditionsForField(
                                    fieldElement, fieldDeclType, atypeFactory);
                    break;
                case AFTER:
                    preOrPostConditionAnnos =
                            methodAnnos.getPostconditionsForField(
                                    fieldElement, fieldDeclType, atypeFactory);
                    break;
                default:
                    throw new BugInCF("Unexpected " + preOrPost);
            }

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
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt, file);

        for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);
            AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
            AnnotatedTypeMirror paramType =
                    getParameterType(methodAnnos, i, paramATM, ve, atypeFactory);
            updateAnnotationSet(paramType, TypeUseLocation.PARAMETER, argATM, paramATM, file);
        }

        AnnotatedDeclaredType argADT = overriddenMethod.getReceiverType();
        if (argADT != null) {
            AnnotatedTypeMirror paramATM =
                    atypeFactory.getAnnotatedType(methodTree).getReceiverType();
            if (paramATM != null) {
                AnnotatedTypeMirror receiver = getReceiverType(methodAnnos, paramATM, atypeFactory);
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
        String file = getFileForElement(methodElt);
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt, file);

        AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(paramElt);
        AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(rhsTree);
        int i = methodElt.getParameters().indexOf(paramElt);
        assert i != -1;
        AnnotatedTypeMirror paramType =
                getParameterType(methodAnnos, i, paramATM, paramElt, atypeFactory);
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

        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        ClassOrInterfaceAnnos classAnnos = getClassAnnos(className, file, enclosingClass);

        AnnotatedTypeMirror lhsATM = atypeFactory.getAnnotatedType(lhsTree);
        AnnotatedTypeMirror fieldType = getFieldType(classAnnos, fieldName, lhsATM, atypeFactory);

        updateAnnotationSet(fieldType, TypeUseLocation.FIELD, rhsATM, lhsATM, file);
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
        String file = getFileForElement(methodElt);

        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt, file);
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
        AnnotatedTypeMirror returnTypeAnnos = getReturnType(methodAnnos, lhsATM, atypeFactory);
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
            CallableDeclarationAnnos overriddenMethodInSuperclass =
                    getMethodAnnos(overriddenMethodElement, superClassFile);
            AnnotatedTypeMirror overriddenMethodReturnType = overriddenMethod.getReturnType();
            AnnotatedTypeMirror storedOverriddenMethodReturnType =
                    getReturnType(
                            overriddenMethodInSuperclass, overriddenMethodReturnType, atypeFactory);

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

        String file = getFileForElement(methodElt);
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt, file);
        if (methodAnnos.declarationAnnotations == null) {
            methodAnnos.declarationAnnotations = new LinkedHashSet<AnnotationMirror>();
        }

        boolean isNewAnnotation = methodAnnos.declarationAnnotations.add(anno);
        if (isNewAnnotation) {
            modifiedFiles.add(file);
        }
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
     *     as modified (needing to be written to disk) source code
     */
    protected void updateAnnotationSet(
            AnnotatedTypeMirror typeToUpdate,
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
            AnnotatedTypeMirror typeToUpdate,
            TypeUseLocation defLoc,
            AnnotatedTypeMirror rhsATM,
            AnnotatedTypeMirror lhsATM,
            String file,
            boolean ignoreIfAnnotated) {
        if (rhsATM instanceof AnnotatedNullType && ignoreNullAssignments) {
            return;
        }

        updateATMWithLUB(rhsATM, typeToUpdate);
        if (lhsATM instanceof AnnotatedTypeVariable) {
            Set<AnnotationMirror> upperAnnos =
                    ((AnnotatedTypeVariable) lhsATM).getUpperBound().getEffectiveAnnotations();
            // If the inferred type is a subtype of the upper bounds of the
            // current type on the source code, halt.
            if (upperAnnos.size() == rhsATM.getAnnotations().size()
                    && atypeFactory
                            .getQualifierHierarchy()
                            .isSubtype(rhsATM.getAnnotations(), upperAnnos)) {
                return;
            }
        }

        updateAnnotationFromATM(rhsATM, lhsATM, typeToUpdate, defLoc, ignoreIfAnnotated);
        modifiedFiles.add(file);
    }

    /**
     * Updates an {@link org.checkerframework.framework.type.AnnotatedTypeMirror} to have the
     * annotations of an {@code AnnotatedTypeMirror} passed as argument. Annotations in the original
     * set that should be ignored (see {@code #shouldIgnore}) are not added to the resulting set.
     * This method also checks if the AnnotatedTypeMirror has explicit annotations in source code,
     * and if that is the case no annotations are added for that location.
     *
     * <p>This method removes from the {@code AnnotatedTypeMirror} all annotations supported by the
     * AnnotatedTypeFactory before inserting new ones. It is assumed that every time this method is
     * called, the new {@code AnnotatedTypeMirror} has a better type estimate for the given
     * location. Therefore, it is not a problem to remove all annotations before inserting the new
     * annotations.
     *
     * @param newATM the type whose annotations will be added to the {@code AnnotatedTypeMirror}
     * @param curATM used to check if the element which will be updated has explicit annotations in
     *     source code
     * @param typeToUpdate the {@code AnnotatedTypeMirror} which will be updated
     * @param defLoc the location where the annotation will be added
     * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
     *     source code
     */
    private void updateAnnotationFromATM(
            AnnotatedTypeMirror newATM,
            AnnotatedTypeMirror curATM,
            AnnotatedTypeMirror typeToUpdate,
            TypeUseLocation defLoc,
            boolean ignoreIfAnnotated) {
        // Clears only the annotations that are supported by atypeFactory.
        // The others stay intact.
        Set<AnnotationMirror> annosToRemove = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : typeToUpdate.getAnnotations()) {
            if (atypeFactory.isSupportedQualifier(anno)) {
                annosToRemove.add(anno);
            }
        }

        // This method may be called consecutive times to modify the same AnnotatedTypeMirror.
        // Each time it is called, the AnnotatedTypeMirror has a better type
        // estimate for the modified AnnotatedTypeMirror. Therefore, it is not a problem to remove
        // all annotations before inserting the new annotations.
        typeToUpdate.removeAnnotations(annosToRemove);

        // Only update the AnnotatedTypeMirror if there are no explicit annotations
        if (curATM.getExplicitAnnotations().isEmpty() || !ignoreIfAnnotated) {
            for (AnnotationMirror am : newATM.getAnnotations()) {
                typeToUpdate.addAnnotation(am);
            }
        } else if (curATM.getKind() == TypeKind.TYPEVAR) {
            // getExplicitAnnotations will be non-empty for type vars whose bounds are explicitly
            // annotated.  So instead, only insert the annotation if there is not primary annotation
            // of the same hierarchy.  #shouldIgnore prevent annotations that are subtypes of type
            // vars upper bound from being inserted.
            for (AnnotationMirror am : newATM.getAnnotations()) {
                if (curATM.getAnnotationInHierarchy(am) != null) {
                    // Don't insert if the type is already has a primary annotation
                    // in the same hierarchy.
                    break;
                }

                typeToUpdate.addAnnotation(am);
            }
        }

        // Recursively update compound type and type variable type if they exist.
        if (newATM.getKind() == TypeKind.ARRAY && curATM.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType newAAT = (AnnotatedArrayType) newATM;
            AnnotatedArrayType oldAAT = (AnnotatedArrayType) curATM;
            AnnotatedArrayType aatToUpdate = (AnnotatedArrayType) typeToUpdate;
            updateAnnotationFromATM(
                    newAAT.getComponentType(),
                    oldAAT.getComponentType(),
                    aatToUpdate.getComponentType(),
                    defLoc,
                    ignoreIfAnnotated);
        }
    }

    /**
     * Updates sourceCodeATM to contain the LUB between sourceCodeATM and ajavaATM, ignoring missing
     * AnnotationMirrors from ajavaATM -- it considers the LUB between an AnnotationMirror am and a
     * missing AnnotationMirror to be am. The results are stored in sourceCodeATM.
     *
     * @param sourceCodeATM the annotated type on the source code
     * @param ajavaATM the annotated type on the ajava file
     */
    private void updateATMWithLUB(AnnotatedTypeMirror sourceCodeATM, AnnotatedTypeMirror ajavaATM) {

        switch (sourceCodeATM.getKind()) {
            case TYPEVAR:
                updateATMWithLUB(
                        ((AnnotatedTypeVariable) sourceCodeATM).getLowerBound(),
                        ((AnnotatedTypeVariable) ajavaATM).getLowerBound());
                updateATMWithLUB(
                        ((AnnotatedTypeVariable) sourceCodeATM).getUpperBound(),
                        ((AnnotatedTypeVariable) ajavaATM).getUpperBound());
                break;
                //        case WILDCARD:
                // Because inferring type arguments is not supported, wildcards won't be encoutered
                //            updatesATMWithLUB(atf, ((AnnotatedWildcardType)
                // sourceCodeATM).getExtendsBound(),
                //                              ((AnnotatedWildcardType)
                // ajavaATM).getExtendsBound());
                //            updatesATMWithLUB(atf, ((AnnotatedWildcardType)
                // sourceCodeATM).getSuperBound(),
                //                              ((AnnotatedWildcardType) ajavaATM).getSuperBound());
                //            break;
            case ARRAY:
                updateATMWithLUB(
                        ((AnnotatedArrayType) sourceCodeATM).getComponentType(),
                        ((AnnotatedArrayType) ajavaATM).getComponentType());
                break;
                // case DECLARED:
                // inferring annotations on type arguments is not supported, so no need to recur on
                // generic types. If this was every implemented, this method would need VisitHistory
                // object to prevent infinite recursion on types such as T extends List<T>.
            default:
                // ATM only has primary annotations
                break;
        }

        // LUB primary annotations
        Set<AnnotationMirror> annosToReplace = new HashSet<>();
        for (AnnotationMirror amSource : sourceCodeATM.getAnnotations()) {
            AnnotationMirror amAjava = ajavaATM.getAnnotationInHierarchy(amSource);
            // amAjava only contains  annotations from the ajava file, so it might be missing
            // an annotation in the hierarchy
            if (amAjava != null) {
                amSource = atypeFactory.getQualifierHierarchy().leastUpperBound(amSource, amAjava);
            }
            annosToReplace.add(amSource);
        }
        sourceCodeATM.replaceAnnotations(annosToReplace);
    }

    /**
     * Reads in the source file containing {@code tree} and creates a wrapper around {@code tree}.
     *
     * @param tree tree for class to add
     */
    public void addClassTree(ClassTree tree) {
        TypeElement element = TreeUtils.elementFromDeclaration(tree);
        String className = getClassName(element);
        if (classToAnnos.containsKey(className)) {
            return;
        }

        TypeElement toplevelClass = toplevelEnclosingClass(element);
        String path = AjavaUtils.getSourceFilePath(toplevelClass);
        addSourceFile(path);
        CompilationUnitAnnos sourceAnnos = sourceToAnnos.get(path);
        TypeDeclaration<?> javaParserNode =
                sourceAnnos.getClassOrInterfaceDeclarationByName(
                        toplevelClass.getSimpleName().toString());
        ClassTree toplevelClassTree = atypeFactory.getTreeUtils().getTree(toplevelClass);
        createWrappersForClass(toplevelClassTree, javaParserNode, sourceAnnos);
    }

    /**
     * Reads in the file at {@code path} and creates and stores a wrapper around its compilation
     * unit.
     *
     * @param path path to source file to read
     */
    private void addSourceFile(String path) {
        if (sourceToAnnos.containsKey(path)) {
            return;
        }

        try {
            CompilationUnit root = StaticJavaParser.parse(new File(path));
            new StringLiteralConcatenateVisitor().visit(root, null);
            CompilationUnitAnnos sourceAnnos = new CompilationUnitAnnos(root);
            sourceToAnnos.put(path, sourceAnnos);
        } catch (FileNotFoundException e) {
            throw new BugInCF("Failed to read java file: " + path, e);
        }
    }

    /**
     * Given a javac tree and JavaParser node representing the same class, creates wrappers around
     * all the classes, fields, and methods in that class.
     *
     * @param javacClass javac tree for class
     * @param javaParserClass JavaParser node corresponding to the same class as {@code javacClass}
     * @param sourceAnnos compilation unit wrapper to add new wrappers to
     */
    private void createWrappersForClass(
            ClassTree javacClass,
            TypeDeclaration<?> javaParserClass,
            CompilationUnitAnnos sourceAnnos) {
        JointJavacJavaParserVisitor visitor =
                new DefaultJointVisitor() {
                    @Override
                    public void processClass(
                            ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode) {
                        addClass(javacTree);
                    }

                    @Override
                    public void processClass(ClassTree javacTree, EnumDeclaration javaParserNode) {
                        addClass(javacTree);
                    }

                    @Override
                    public void processNewClass(
                            NewClassTree javacTree, ObjectCreationExpr javaParserNode) {
                        if (javacTree.getClassBody() != null) {
                            addClass(javacTree.getClassBody());
                        }
                    }

                    /**
                     * Creates a wrapper around the class for {@code tree} and stores it.
                     *
                     * @param tree tree to add
                     */
                    private void addClass(ClassTree tree) {
                        TypeElement classElt = TreeUtils.elementFromDeclaration(tree);
                        String className = getClassName(classElt);
                        ClassOrInterfaceAnnos typeWrapper = new ClassOrInterfaceAnnos();
                        if (!classToAnnos.containsKey(className)) {
                            classToAnnos.put(className, typeWrapper);
                        }

                        sourceAnnos.types.add(typeWrapper);
                    }

                    @Override
                    public void processMethod(
                            MethodTree javacTree, MethodDeclaration javaParserNode) {
                        addCallableDeclaration(javacTree, javaParserNode);
                    }

                    @Override
                    public void processMethod(
                            MethodTree javacTree, ConstructorDeclaration javaParserNode) {
                        addCallableDeclaration(javacTree, javaParserNode);
                    }

                    /**
                     * Creates a wrapper around {@code javacTree} with the corresponding declaration
                     * {@code javaParserNode} and stores it.
                     *
                     * @param javacTree javac tree for declaration to add
                     * @param javaParserNode JavaParser node for the same class as {@code javacTree}
                     */
                    private void addCallableDeclaration(
                            MethodTree javacTree, CallableDeclaration<?> javaParserNode) {
                        ExecutableElement elt = TreeUtils.elementFromDeclaration(javacTree);
                        String className = getEnclosingClassName(elt);
                        ClassOrInterfaceAnnos enclosingClass = getClassAnnos(className, null, null);
                        String executableName = JVMNames.getJVMMethodSignature(javacTree);
                        if (!enclosingClass.callableDeclarations.containsKey(executableName)) {
                            enclosingClass.callableDeclarations.put(
                                    executableName, new CallableDeclarationAnnos(javaParserNode));
                        }
                    }

                    @Override
                    public void processVariable(
                            VariableTree javacTree, VariableDeclarator javaParserNode) {
                        // This seems to occur when javacTree is a local variable in the second
                        // class located in a source file. If this check returns false, then the
                        // below call to TreeUtils.elementFromDeclaration causes a crash.
                        if (TreeUtils.elementFromTree(javacTree) == null) {
                            return;
                        }

                        VariableElement elt = TreeUtils.elementFromDeclaration(javacTree);
                        if (!elt.getKind().isField()) {
                            return;
                        }

                        String className = getEnclosingClassName(elt);
                        ClassOrInterfaceAnnos enclosingClass = getClassAnnos(className, null, null);
                        String fieldName = javacTree.getName().toString();
                        if (!enclosingClass.fields.containsKey(fieldName)) {
                            enclosingClass.fields.put(fieldName, new FieldAnnos(javaParserNode));
                        }
                    }
                };
        visitor.visitClass(javacClass, javaParserClass);
    }

    /**
     * Calls {@link #addSourceFile(String)} for the file containing the given element.
     *
     * @param element element for the source file to add
     * @return path of the file containing {@code element}
     */
    private String addClassesForElement(Element element) {
        if (!ElementUtils.isElementFromSourceCode(element)) {
            throw new BugInCF("Adding source file for non-source element: " + element);
        }

        TypeElement toplevelClass = toplevelEnclosingClass(element);
        String path = AjavaUtils.getSourceFilePath(toplevelClass);
        if (classToAnnos.containsKey(getClassName(toplevelClass))) {
            return path;
        }

        addSourceFile(path);
        CompilationUnitAnnos sourceAnnos = sourceToAnnos.get(path);
        ClassTree toplevelClassTree =
                (ClassTree) atypeFactory.declarationFromElement(toplevelClass);
        TypeDeclaration<?> javaParserNode =
                sourceAnnos.getClassOrInterfaceDeclarationByName(
                        toplevelClass.getSimpleName().toString());
        createWrappersForClass(toplevelClassTree, javaParserNode, sourceAnnos);
        return path;
    }

    /**
     * Adds an explicit receiver type to a JavaParser method declaration.
     *
     * @param methodDeclaration declaration to add a receiver to
     */
    private static void addExplicitReceiver(MethodDeclaration methodDeclaration) {
        if (methodDeclaration.getReceiverParameter().isPresent()) {
            return;
        }

        if (!methodDeclaration.getParentNode().isPresent()) {
            return;
        }

        com.github.javaparser.ast.Node parent = methodDeclaration.getParentNode().get();
        if (!(parent instanceof TypeDeclaration)) {
            return;
        }

        TypeDeclaration<?> parentDecl = (TypeDeclaration<?>) parent;
        ClassOrInterfaceType receiver = new ClassOrInterfaceType();
        receiver.setName(parentDecl.getName());
        if (parentDecl.isClassOrInterfaceDeclaration()) {
            ClassOrInterfaceDeclaration parentClassDecl =
                    parentDecl.asClassOrInterfaceDeclaration();
            if (!parentClassDecl.getTypeParameters().isEmpty()) {
                NodeList<Type> typeArgs = new NodeList<>();
                for (TypeParameter typeParam : parentClassDecl.getTypeParameters()) {
                    ClassOrInterfaceType typeArg = new ClassOrInterfaceType();
                    typeArg.setName(typeParam.getNameAsString());
                    typeArgs.add(typeArg);
                }

                receiver.setTypeArguments(typeArgs);
            }
        }

        methodDeclaration.setReceiverParameter(new ReceiverParameter(receiver, "this"));
    }

    /**
     * Transfers all annotations for {@code annotatedType} and its nested types to {@code target},
     * which is the JavaParser node representing the same type. Does nothing if {@code
     * annotatedType} is null.
     *
     * @param annotatedType type to transfer annotations from
     * @param target type to transfer annotation to, must represent the same type as {@code
     *     annotatedType}
     */
    private static void transferAnnotations(
            @Nullable AnnotatedTypeMirror annotatedType, Type target) {
        if (annotatedType == null) {
            return;
        }

        target.accept(new AnnotationTransferVisitor(), annotatedType);
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
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
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
        return getClassName(ElementUtils.enclosingClass(variableElement));
    }

    /**
     * Returns the "flatname" of the class enclosing {@code localVariableNode}
     *
     * @param localVariableNode the {@link LocalVariableNode}
     * @return the "flatname" of the class enclosing {@code localVariableNode}
     */
    @SuppressWarnings({
        "signature", // https://tinyurl.com/cfissue/3094
        "UnusedMethod" // remove this method
    })
    private @BinaryName String getEnclosingClassName(LocalVariableNode localVariableNode) {
        return ((ClassSymbol) ElementUtils.enclosingClass(localVariableNode.getElement()))
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
        if (ElementUtils.enclosingClass(element) == null) {
            return (TypeElement) element;
        }

        TypeElement result = ElementUtils.enclosingClass(element);
        // TODO JWAATAJA: This loop calls enclosingClass twice.  Please make it just one
        // invocation.  You could use a local variable.
        while (ElementUtils.enclosingClass(result) != null
                // TODO JWAATAJA: Why is the test against equality necessary?  The contract of
                // ElementUtils.enclosingClass() does not permit it to return its argument.
                && !ElementUtils.enclosingClass(result).equals(result)) {
            result = ElementUtils.enclosingClass(result);
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

    ///
    /// Writing to a file
    ///

    // The prepare*ForWriting hooks are needed in addition to the postProcessClassTree hook because
    // a scene may be modifed and written at any time, including before or after
    // postProcessClassTree is called.

    /**
     * Side-effects the CompilationUnitAnnos to make any desired changes before writing to a file.
     *
     * @param compilationUnitAnnos the CompilationUnitAnnos to modify
     */
    public void prepareCompilationUnitForWriting(CompilationUnitAnnos compilationUnitAnnos) {
        for (ClassOrInterfaceAnnos type : compilationUnitAnnos.types) {
            prepareClassForWriting(type);
        }
    }

    /**
     * Side-effects the class annotations to make any desired changes before writing to a file.
     *
     * @param classAnnos the class annotations to modify
     */
    public void prepareClassForWriting(ClassOrInterfaceAnnos classAnnos) {
        for (Map.Entry<String, CallableDeclarationAnnos> methodEntry :
                classAnnos.callableDeclarations.entrySet()) {
            atypeFactory.prepareMethodForWriting(methodEntry.getValue());
        }
    }

    @Override
    public void writeResultsToFile(OutputFormat outputFormat, BaseTypeChecker checker) {
        if (outputFormat != OutputFormat.AJAVA) {
            throw new BugInCF("WholeProgramInferenceJavaParser used with format " + outputFormat);
        }

        File outputDir = new File(AJAVA_FILES_PATH);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (String path : modifiedFiles) {
            CompilationUnitAnnos root = sourceToAnnos.get(path);
            prepareCompilationUnitForWriting(root);
            root.transferAnnotations();
            String packageDir = AJAVA_FILES_PATH;
            if (root.declaration.getPackageDeclaration().isPresent()) {
                packageDir +=
                        File.separator
                                + root.declaration
                                        .getPackageDeclaration()
                                        .get()
                                        .getNameAsString()
                                        .replaceAll("\\.", File.separator);
            }

            File packageDirFile = new File(packageDir);
            if (!packageDirFile.exists()) {
                packageDirFile.mkdirs();
            }

            String name = new File(path).getName();
            if (name.endsWith(".java")) {
                name = name.substring(0, name.length() - ".java".length());
            }

            name += "-" + checker.getClass().getCanonicalName() + ".ajava";
            String outputPath = packageDir + File.separator + name;
            try {
                FileWriter writer = new FileWriter(outputPath);

                // JavaParser can output using lexical preserving printing, which writes the file
                // such that its formatting is close to the original source file it was parsed from
                // as possible. Currently, this feature is very buggy and crashes when adding
                // annotations in certain locations. This implementation could be used instead if
                // it's fixed in JavaParser.
                // LexicalPreservingPrinter.print(root.declaration, writer);

                PrettyPrinter prettyPrinter = new PrettyPrinter(new PrettyPrinterConfiguration());
                writer.write(prettyPrinter.print(root.declaration));
                writer.close();
            } catch (IOException e) {
                throw new BugInCF("Error while writing ajava file", e);
            }
        }

        modifiedFiles.clear();
    }

    ///
    /// Storing annotations
    ///

    /**
     * Stores the JavaParser node for a compilation unit and the list of wrappers for the classes
     * and interfaces in that compilation unit.
     */
    private static class CompilationUnitAnnos {
        /** Compilation unit being wrapped. */
        public CompilationUnit declaration;
        /** Wrappers for classes and interfaces in {@code declaration} */
        public List<ClassOrInterfaceAnnos> types;

        /**
         * Constructs a wrapper around the given declaration.
         *
         * @param declaration compilation unit to wrap
         */
        public CompilationUnitAnnos(CompilationUnit declaration) {
            this.declaration = declaration;
            types = new ArrayList<>();
        }

        /**
         * Transfers all annotations inferred by whole program inference for the wrapped compilation
         * unit to their corresponding JavaParser locations.
         */
        public void transferAnnotations() {
            ClearAnnotationsVisitor annotationClearer = new ClearAnnotationsVisitor();
            declaration.accept(annotationClearer, null);
            for (ClassOrInterfaceAnnos typeAnnos : types) {
                typeAnnos.transferAnnotations();
            }
        }

        /**
         * Returns the top level type declaration in the compilation unit with {@code name}.
         *
         * @param name name of type declaration
         * @return the type declaration with {@code name} in the wrapped compilation unit
         */
        public TypeDeclaration<?> getClassOrInterfaceDeclarationByName(String name) {
            return AjavaUtils.getTypeDeclarationByName(declaration, name);
        }
    }

    /**
     * Stores wrappers for the locations where annotations may be inferred in a class or interface.
     */
    private static class ClassOrInterfaceAnnos {
        /**
         * Mapping from JVM method signatures to the wrapper containing the corresponding
         * executable.
         */
        public Map<String, CallableDeclarationAnnos> callableDeclarations;
        /** Mapping from field names to wrappers for those fields. */
        public Map<String, FieldAnnos> fields;

        /** Creates an empty class or interface wrapper. */
        public ClassOrInterfaceAnnos() {
            callableDeclarations = new HashMap<>();
            fields = new HashMap<>();
        }

        /**
         * Transfers all annotations inferred by whole program inference for the methods and fields
         * in the wrapper class or interface to their corresponding JavaParser locations.
         */
        public void transferAnnotations() {
            for (CallableDeclarationAnnos callableAnnos : callableDeclarations.values()) {
                callableAnnos.transferAnnotations();
            }

            for (FieldAnnos field : fields.values()) {
                field.transferAnnotations();
            }
        }

        @Override
        public String toString() {
            return "ClassOrInterfaceAnnos [callableDeclarations="
                    + callableDeclarations
                    + ", fields="
                    + fields
                    + "]";
        }
    }

    /**
     * Stores the JavaParser node for a method or constructor and the annotations that have been
     * inferred about its parameters and return type.
     */
    public class CallableDeclarationAnnos {
        /** Wrapped method or constructor declaration. */
        public CallableDeclaration<?> declaration;
        /** Path to file containing the declaration. */
        public String file;
        /**
         * Inferred annotations for the return type, if the declaration represents a method.
         * Initialized on first usage.
         */
        public @Nullable AnnotatedTypeMirror returnType;
        /**
         * Inferred annotations for the receiver type, if the declaration represents a method.
         * Initialized on first usage.
         */
        public @Nullable AnnotatedTypeMirror receiverType;
        /**
         * Inferred annotations for parameter types. Initialized the first time any parameter is
         * accessed and each parameter is initialized the first time it's accessed.
         */
        public @Nullable List<@Nullable AnnotatedTypeMirror> parameterTypes;
        /** Annotations on the callable declaration. */
        public @Nullable Set<AnnotationMirror> declarationAnnotations;

        /**
         * Mapping from VariableElements for fields to an AnnotatedTypeMirror containing the
         * inferred preconditions on that field.
         */
        public @Nullable Map<VariableElement, AnnotatedTypeMirror> fieldToPreconditions;
        /**
         * Mapping from VariableElements for fields to an AnnotatedTypeMirror containing the
         * inferred postconditions on that field.
         */
        public @Nullable Map<VariableElement, AnnotatedTypeMirror> fieldToPostconditions;
        /** Inferred contracts for the callable declaration. */
        public @Nullable List<AnnotationMirror> contracts;

        /**
         * Creates a wrapper for the given method or constructor declaration.
         *
         * @param declaration method or constructor declaration to wrap
         */
        public CallableDeclarationAnnos(CallableDeclaration<?> declaration) {
            this.declaration = declaration;
            this.returnType = null;
            this.receiverType = null;
            this.parameterTypes = null;
            this.declarationAnnotations = null;
            this.fieldToPreconditions = null;
            this.fieldToPostconditions = null;
            this.contracts = null;
        }

        /**
         * Returns the inferred type for the parameter at the given index. If necessary, initializes
         * the {@code AnnotatedTypeMirror} for that location using {@code type} and {@code atf} to a
         * wrapper around the base type for the parameter.
         *
         * @param type type for the parameter at {@code index}, used for initializing the returned
         *     {@code AnnotatedTypeMirror} the first time it's accessed
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @param index index of the parameter to return the inferred annotations of
         * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the
         *     parameter at the given index
         */
        public AnnotatedTypeMirror getParameterType(
                AnnotatedTypeMirror type, int index, AnnotatedTypeFactory atf) {
            if (parameterTypes == null) {
                parameterTypes = new ArrayList<>();
                for (int i = 0; i < declaration.getParameters().size(); i++) {
                    parameterTypes.add(null);
                }
            }

            if (parameterTypes.get(index) == null) {
                parameterTypes.set(
                        index,
                        AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false));
            }

            return parameterTypes.get(index);
        }

        /**
         * If this wrapper holds a method, returns the inferred type of the receiver type. If
         * necessary, initializes the {@code AnnotatedTypeMirror} for that location using {@code
         * type} and {@code atf} to a wrapper around the base type for the receiver type.
         *
         * @param type base type for the receiver type, used for initializing the returned {@code
         *     AnnotatedTypeMirror} the first time it's accessed
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the
         *     receiver type
         */
        public AnnotatedTypeMirror getReceiverType(
                AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (receiverType == null) {
                receiverType = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
            }

            return receiverType;
        }

        /**
         * If this wrapper holds a method, returns the inferred type of the return type. If
         * necessary, initializes the {@code AnnotatedTypeMirror} for that location using {@code
         * type} and {@code atf} to a wrapper around the base type for the return type.
         *
         * @param type base type for the return type, used for initializing the returned {@code
         *     AnnotatedTypeMirror} the first time it's accessed
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the return
         *     type
         */
        public AnnotatedTypeMirror getReturnType(
                AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (returnType == null) {
                returnType = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
            }

            return returnType;
        }

        /**
         * Returns an AnnotatedTypeMirror containing the preconditions for the given field.
         * Initializes {@code fieldToPreconditions} and the entry for the field if necessary.
         *
         * @param field VariableElement for a field in the enclosing class for this method
         * @param type base type for the return type, used for initializing the returned {@code
         *     AnnotatedTypeMirror} the first time it's accessed
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
         *     preconditions for the given field
         */
        public AnnotatedTypeMirror getPreconditionsForField(
                VariableElement field, AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (fieldToPreconditions == null) {
                fieldToPreconditions = new HashMap<>();
            }

            if (!fieldToPreconditions.containsKey(field)) {
                AnnotatedTypeMirror preconditionsType =
                        AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
                fieldToPreconditions.put(field, preconditionsType);
            }

            return fieldToPreconditions.get(field);
        }

        /**
         * Returns an AnnotatedTypeMirror containing the postconditions for the given field.
         * Initializes {@code fieldToPreconditions} and the entry for the field if necessary.
         *
         * @param field VariableElement for a field in the enclosing class for this method
         * @param type base type for the return type, used for initializing the returned {@code
         *     AnnotatedTypeMirror} the first time it's accessed
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
         *     postconditions for the given field
         */
        public AnnotatedTypeMirror getPostconditionsForField(
                VariableElement field, AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (fieldToPostconditions == null) {
                fieldToPostconditions = new HashMap<>();
            }

            if (!fieldToPostconditions.containsKey(field)) {
                AnnotatedTypeMirror postconditionsType =
                        AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
                fieldToPostconditions.put(field, postconditionsType);
            }

            return fieldToPostconditions.get(field);
        }

        /**
         * Transfers all annotations inferred by whole program inference for the return type,
         * receiver type, and parameter types for the wrapped declaration to their corresponding
         * JavaParser locations.
         */
        public void transferAnnotations() {
            if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>) {
                GenericAnnotatedTypeFactory<?, ?, ?, ?> genericAtf =
                        (GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory;
                for (AnnotationMirror contractAnno : genericAtf.getContractAnnotations(this)) {
                    declaration.addAnnotation(
                            AnnotationConversion.annotationMirrorToAnnotationExpr(contractAnno));
                }
            }

            if (declarationAnnotations != null) {
                for (AnnotationMirror annotation : declarationAnnotations) {
                    declaration.addAnnotation(
                            AnnotationConversion.annotationMirrorToAnnotationExpr(annotation));
                }
            }

            if (returnType != null) {
                // If a return type exists, then the declaration must be a method, not a
                // constructor.
                WholeProgramInferenceJavaParser.transferAnnotations(
                        returnType, declaration.asMethodDeclaration().getType());
            }

            if (receiverType != null) {
                addExplicitReceiver(declaration.asMethodDeclaration());
                // The receiver won't be present for an anonymous class.
                if (declaration.getReceiverParameter().isPresent()) {
                    WholeProgramInferenceJavaParser.transferAnnotations(
                            receiverType, declaration.getReceiverParameter().get().getType());
                }
            }

            if (parameterTypes == null) {
                return;
            }

            for (int i = 0; i < parameterTypes.size(); i++) {
                WholeProgramInferenceJavaParser.transferAnnotations(
                        parameterTypes.get(i), declaration.getParameter(i).getType());
            }
        }

        @Override
        public String toString() {
            return "CallableDeclarationAnnos [declaration="
                    + declaration
                    + ", file="
                    + file
                    + ", parameterTypes="
                    + parameterTypes
                    + ", receiverType="
                    + receiverType
                    + ", returnType="
                    + returnType
                    + "]";
        }
    }

    /**
     * Stores the JavaParser node for a field and the annotations that have been inferred for it.
     */
    private static class FieldAnnos {
        /** Wrapped field declaration. */
        public VariableDeclarator declaration;
        /** Inferred type for field, initialized the first time it's accessed. */
        public @Nullable AnnotatedTypeMirror type;

        /**
         * Creates a wrapper for the given field declaration.
         *
         * @param declaration field declaration to wrap
         */
        public FieldAnnos(VariableDeclarator declaration) {
            this.declaration = declaration;
            type = null;
        }

        /**
         * Returns the inferred type of the field. If necessary, initializes the {@code
         * AnnotatedTypeMirror} for that location using {@code type} and {@code atf} to a wrapper
         * around the base type for the field.
         *
         * @param type base type for the field, used for initializing the returned {@code
         *     AnnotatedTypeMirror} the first time it's accessed
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the field
         */
        public AnnotatedTypeMirror getType(AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (this.type == null) {
                this.type = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
            }

            return this.type;
        }

        /**
         * Transfers all annotations inferred by whole program inference on this field to the
         * JavaParser nodes for that field.
         */
        public void transferAnnotations() {
            if (type == null) {
                return;
            }

            Type newType = (Type) declaration.getType().accept(new CloneVisitor(), null);
            WholeProgramInferenceJavaParser.transferAnnotations(type, newType);
            declaration.setType(newType);
        }

        @Override
        public String toString() {
            return "FieldAnnos [declaration=" + declaration + ", type=" + type + "]";
        }
    }
}
