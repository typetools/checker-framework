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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.ajava.AjavaUtils;
import org.checkerframework.framework.ajava.AnnotationConversion;
import org.checkerframework.framework.ajava.AnnotationTransferVisitor;
import org.checkerframework.framework.ajava.ClearAnnotationsVisitor;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.ajava.JointJavacJavaParserVisitor;
import org.checkerframework.framework.ajava.StringLiteralCombineVisitor;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import scenelib.annotations.util.JVMNames;

/**
 * This is an implementation of {@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInference} that reads in the files
 * it processes using JavaParser and stores annotations directly with the JavaParser nodes they
 * apply to. As a result, it only outputs to ajava files. This class behaves identically to {@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes}. See that class
 * for documentation on behavior.
 */
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
public class WholeProgramInferenceJavaParser implements WholeProgramInference {
    /**
     * Directory where .ajava files will be written to and read from. This directory is relative to
     * where the CF's javac command is executed.
     */
    public static final String AJAVA_FILES_PATH =
            "build" + File.separator + "whole-program-inference" + File.separator;

    /**
     * Mapping of binary class names to the wrapper containing those classes. Contains all classes
     * in source files added for inference.
     */
    private Map<@BinaryName String, ClassOrInterfaceWrapper> classes;

    /**
     * The list of paths to files containing classes for which an annotation has been inferred since
     * the last time files were written to disk.
     */
    private Set<String> modifiedFiles;

    /**
     * Mapping of source file paths to the collection of wrappers for compilation units parsed from
     * that file.
     */
    private Map<String, CompilationUnitWrapper> sourceFiles;

    /** For calling declarationFromElement. */
    private AnnotatedTypeFactory atypeFactory;

    /** Indicates whether assignments where the rhs is null should be ignored. */
    private final boolean ignoreNullAssignments;

    /**
     * Constructs a {@code WholeProgramInferenceJavaParser} which has not yet inferred any
     * annotations.
     *
     * @param atypeFactory annotated type factory for type system to use
     * @param ignoreNullAssignments indicates whether assignments where the rhs is null should be
     *     ignored
     */
    public WholeProgramInferenceJavaParser(
            AnnotatedTypeFactory atypeFactory, boolean ignoreNullAssignments) {
        this.atypeFactory = atypeFactory;
        classes = new HashMap<>();
        modifiedFiles = new HashSet<>();
        sourceFiles = new HashMap<>();
        this.ignoreNullAssignments = ignoreNullAssignments;
    }

    @Override
    public void updateFromObjectCreation(
            ObjectCreationNode objectCreationNode,
            ExecutableElement constructorElt,
            AnnotatedTypeFactory atf) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(constructorElt)) {
            return;
        }

        String file = addClassesForElement(constructorElt);
        String className = getEnclosingClassName(constructorElt);
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper constructor =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(constructorElt));
        List<Node> arguments = objectCreationNode.getArguments();
        updateInferredExecutableParameterTypes(constructorElt, atf, file, constructor, arguments);
    }

    @Override
    public void updateFromMethodInvocation(
            MethodInvocationNode methodInvNode,
            Tree receiverTree,
            ExecutableElement methodElt,
            AnnotatedTypeFactory atf) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String file = addClassesForElement(methodElt);
        String className = getEnclosingClassName(methodElt);
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        // TODO: Fix this, find a more precise way to test.
        if (method == null && methodElt.getSimpleName().contentEquals("valueOf")) {
            return;
        }

        List<Node> arguments = methodInvNode.getArguments();
        updateInferredExecutableParameterTypes(methodElt, atf, file, method, arguments);
    }

    /**
     * Given an executable such as method or constructor invocation using the given list of
     * arguments, updates the inferred types of each parameter.
     *
     * @param methodElt element for the executable that was called
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param file path to the source file containing the executable
     * @param executable the wrapper containing the currently stored annotations for the executable
     * @param arguments arguments of the invocation
     */
    private void updateInferredExecutableParameterTypes(
            ExecutableElement methodElt,
            AnnotatedTypeFactory atf,
            String file,
            CallableDeclarationWrapper executable,
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
            updateAnnotationSetAtLocation(
                    executable.getParameterType(argATM, atf, i),
                    atf,
                    file,
                    argATM,
                    paramATM,
                    TypeUseLocation.PARAMETER);
        }
    }

    @Override
    public void updateFromOverride(
            MethodTree methodTree,
            ExecutableElement methodElt,
            AnnotatedExecutableType overriddenMethod,
            AnnotatedTypeFactory atf) {
        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String file = addClassesForElement(methodElt);
        String className = getEnclosingClassName(methodElt);
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(ve);

            AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
            AnnotatedTypeMirror param = method.getParameterType(argATM, atf, i);
            updateAnnotationSetAtLocation(
                    param, atf, file, argATM, paramATM, TypeUseLocation.PARAMETER);
        }

        AnnotatedDeclaredType argADT = overriddenMethod.getReceiverType();
        if (argADT != null) {
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(methodTree).getReceiverType();
            if (paramATM != null) {
                AnnotatedTypeMirror receiver = method.getReceiverType(paramATM, atf);
                updateAnnotationSetAtLocation(
                        receiver, atf, file, argADT, paramATM, TypeUseLocation.RECEIVER);
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
        // Don't infer types for code that isn't presented as source.
        if (!isElementFromSourceCode(lhs)) {
            return;
        }

        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodTree);
        String file = addClassesForElement(methodElt);
        String className = getEnclosingClassName(lhs);
        ClassOrInterfaceWrapper clazz = classes.get(className);
        // TODO: Could this be a constructor?
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
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
                AnnotatedTypeMirror param = method.getParameterType(paramATM, atf, i);
                updateAnnotationSetAtLocation(
                        param, atf, file, argATM, paramATM, TypeUseLocation.PARAMETER);
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

        // Do not attempt to infer types for fields that do not have valid
        // names. For example, compiler-generated temporary variables will
        // have invalid names. Recording facts about fields with
        // invalid names causes jaif-based WPI to crash when reading the .jaif
        // file, and stub-based WPI to generate unparseable stub files.
        // See https://github.com/typetools/checker-framework/issues/3442
        if (!SourceVersion.isIdentifier(fieldName)) {
            return;
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

        // Don't infer types for code that isn't presented as source.
        if (!ElementUtils.isElementFromSourceCode(enclosingClass)) {
            return;
        }

        String file = addClassesForElement(element);
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        ClassOrInterfaceWrapper clazz = classes.get(className);

        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        AnnotatedTypeMirror field = clazz.fields.get(fieldName).getType(lhsATM, atf);
        // TODO: For a primitive such as long, this is yielding just @GuardedBy rather than
        // @GuardedBy({}).
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());
        updateAnnotationSetAtLocation(field, atf, file, rhsATM, lhsATM, TypeUseLocation.FIELD);
    }

    @Override
    public void updateFromReturn(
            ReturnNode retNode,
            ClassSymbol classSymbol,
            MethodTree methodTree,
            Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods,
            AnnotatedTypeFactory atf) {
        // Don't infer types for code that isn't presented as source.
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

        ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodTree);
        String file = addClassesForElement(methodElt);
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = classSymbol.flatname.toString();
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(methodTree).getReturnType();

        // Type of the expression returned
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(retNode.getTree().getExpression());
        AnnotatedTypeMirror returnType = method.getReturnType(lhsATM, atf);
        updateAnnotationSetAtLocation(
                returnType, atf, file, rhsATM, lhsATM, TypeUseLocation.RETURN);

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
                            atf.getProcessingEnv().getTypeUtils(),
                            atf,
                            superclassDecl,
                            overriddenMethodElement);

            // TODO: is it superClass or superclass?
            String superClassFile = addClassesForElement(overriddenMethodElement);
            String superClassName = getEnclosingClassName(overriddenMethodElement);
            ClassOrInterfaceWrapper superClazz = classes.get(superClassName);
            CallableDeclarationWrapper overriddenMethodInSuperclass =
                    superClazz.callableDeclarations.get(
                            JVMNames.getJVMMethodSignature(overriddenMethodElement));
            AnnotatedTypeMirror overriddenMethodReturnType = overriddenMethod.getReturnType();
            AnnotatedTypeMirror storedOverriddenMethodReturnType =
                    overriddenMethodInSuperclass.getReturnType(overriddenMethodReturnType, atf);

            updateAnnotationSetAtLocation(
                    storedOverriddenMethodReturnType,
                    atf,
                    superClassFile,
                    rhsATM,
                    overriddenMethodReturnType,
                    TypeUseLocation.RETURN);
        }
    }

    @Override
    public void addMethodDeclarationAnnotation(ExecutableElement methodElt, AnnotationMirror anno) {

        // Do not infer types for library code, only for type-checked source code.
        if (!ElementUtils.isElementFromSourceCode(methodElt)) {
            return;
        }

        String file = addClassesForElement(methodElt);
        String className = getEnclosingClassName(methodElt);
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        if (method.declarationAnnotations == null) {
            method.declarationAnnotations = new LinkedHashSet<AnnotationMirror>();
        }

        boolean isNewAnnotation = method.declarationAnnotations.add(anno);
        if (isNewAnnotation) {
            modifiedFiles.add(file);
        }
    }

    @Override
    public void writeResultsToFile(OutputFormat format, BaseTypeChecker checker) {
        if (format != OutputFormat.AJAVA) {
            throw new BugInCF("WholeProgramInferenceJavaParser used with non-ajava format");
        }

        File outputDir = new File(AJAVA_FILES_PATH);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (String path : modifiedFiles) {
            CompilationUnitWrapper root = sourceFiles.get(path);
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
     * @param typeToUpdate the {@code AnnotatedTypeMirror} for the location which will be modified
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param file path to the source file containing the executable
     * @param rhsATM the RHS of the annotated type on the source code
     * @param lhsATM the LHS of the annotated type on the source code
     * @param defLoc the location where the annotation will be added
     */
    protected void updateAnnotationSetAtLocation(
            AnnotatedTypeMirror typeToUpdate,
            AnnotatedTypeFactory atf,
            String file,
            AnnotatedTypeMirror rhsATM,
            AnnotatedTypeMirror lhsATM,
            TypeUseLocation defLoc) {
        if (rhsATM instanceof AnnotatedNullType && ignoreNullAssignments) {
            return;
        }

        updateATMWithLUB(atf, rhsATM, typeToUpdate);
        if (lhsATM instanceof AnnotatedTypeVariable) {
            Set<AnnotationMirror> upperAnnos =
                    ((AnnotatedTypeVariable) lhsATM).getUpperBound().getEffectiveAnnotations();
            // If the inferred type is a subtype of the upper bounds of the
            // current type on the source code, halt.
            if (upperAnnos.size() == rhsATM.getAnnotations().size()
                    && atf.getQualifierHierarchy().isSubtype(rhsATM.getAnnotations(), upperAnnos)) {
                return;
            }
        }

        updateTypeElementFromATM(rhsATM, lhsATM, atf, typeToUpdate, defLoc);
        modifiedFiles.add(file);
    }

    /**
     * Updates an {@link org.checkerframework.framework.type.AnnotatedTypeMirror} to have the
     * annotations of an {@code AnnotatedTypeMirror} passed as argument. Annotations in the original
     * set that should be ignored (see {@code #shouldIgnore}) are not added to the resulting set.
     * This method also checks if the AnnotatedTypeMirror has explicit annotations in source code,
     * and if that is the case no annotations are added for that location.
     *
     * <p>This method removes from the {@code AnnotatedTypeMirror} all annotations supported by
     * {@code atf} before inserting new ones. It is assumed that every time this method is called,
     * the new {@code AnnotatedTypeMirror} has a better type estimate for the given location.
     * Therefore, it is not a problem to remove all annotations before inserting the new
     * annotations.
     *
     * @param newATM the type whose annotations will be added to the {@code AnnotatedTypeMirror}
     * @param curATM used to check if the element which will be updated has explicit annotations in
     *     source code
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param typeToUpdate the {@code AnnotatedTypeMirror} which will be updated
     * @param defLoc the location where the annotation will be added
     */
    private void updateTypeElementFromATM(
            AnnotatedTypeMirror newATM,
            AnnotatedTypeMirror curATM,
            AnnotatedTypeFactory atf,
            AnnotatedTypeMirror typeToUpdate,
            TypeUseLocation defLoc) {
        // Clears only the annotations that are supported by atf.
        // The others stay intact.
        Set<AnnotationMirror> annosToRemove = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : typeToUpdate.getAnnotations()) {
            if (atf.isSupportedQualifier(anno)) {
                annosToRemove.add(anno);
            }
        }

        // This method may be called consecutive times for the same ATypeElement.
        // Each time it is called, the AnnotatedTypeMirror has a better type
        // estimate for the ATypeElement. Therefore, it is not a problem to remove
        // all annotations before inserting the new annotations.
        typeToUpdate.removeAnnotations(annosToRemove);

        // Only update the ATypeElement if there are no explicit annotations
        if (curATM.getExplicitAnnotations().isEmpty()) {
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
            updateTypeElementFromATM(
                    newAAT.getComponentType(),
                    oldAAT.getComponentType(),
                    atf,
                    aatToUpdate.getComponentType(),
                    defLoc);
        }
    }

    /**
     * Updates sourceCodeATM to contain the LUB between sourceCodeATM and jaifATM, ignoring missing
     * AnnotationMirrors from jaifATM -- it considers the LUB between an AnnotationMirror am and a
     * missing AnnotationMirror to be am. The results are stored in sourceCodeATM.
     *
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param sourceCodeATM the annotated type on the source code
     * @param jaifATM the annotated type on the .jaif file
     */
    private void updateATMWithLUB(
            AnnotatedTypeFactory atf,
            AnnotatedTypeMirror sourceCodeATM,
            AnnotatedTypeMirror jaifATM) {

        switch (sourceCodeATM.getKind()) {
            case TYPEVAR:
                updateATMWithLUB(
                        atf,
                        ((AnnotatedTypeVariable) sourceCodeATM).getLowerBound(),
                        ((AnnotatedTypeVariable) jaifATM).getLowerBound());
                updateATMWithLUB(
                        atf,
                        ((AnnotatedTypeVariable) sourceCodeATM).getUpperBound(),
                        ((AnnotatedTypeVariable) jaifATM).getUpperBound());
                break;
                //        case WILDCARD:
                // Because inferring type arguments is not supported, wildcards won't be encoutered
                //            updatesATMWithLUB(atf, ((AnnotatedWildcardType)
                // sourceCodeATM).getExtendsBound(),
                //                              ((AnnotatedWildcardType)
                // jaifATM).getExtendsBound());
                //            updatesATMWithLUB(atf, ((AnnotatedWildcardType)
                // sourceCodeATM).getSuperBound(),
                //                              ((AnnotatedWildcardType) jaifATM).getSuperBound());
                //            break;
            case ARRAY:
                updateATMWithLUB(
                        atf,
                        ((AnnotatedArrayType) sourceCodeATM).getComponentType(),
                        ((AnnotatedArrayType) jaifATM).getComponentType());
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
            AnnotationMirror amJaif = jaifATM.getAnnotationInHierarchy(amSource);
            // amJaif only contains  annotations from the jaif, so it might be missing
            // an annotation in the hierarchy
            if (amJaif != null) {
                amSource = atf.getQualifierHierarchy().leastUpperBound(amSource, amJaif);
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
        if (classes.containsKey(className)) {
            return;
        }

        TypeElement mostEnclosing = mostEnclosingClass(element);
        String path = AjavaUtils.getSourceFilePath(mostEnclosing);
        addSourceFile(path);
        CompilationUnitWrapper wrapper = sourceFiles.get(path);
        TypeDeclaration<?> javaParserNode =
                wrapper.getClassOrInterfaceDeclarationByName(
                        mostEnclosing.getSimpleName().toString());
        // ClassTree mostEnclosingTree = (ClassTree)
        // atypeFactory.declarationFromElement(mostEnclosing);
        ClassTree mostEnclosingTree = atypeFactory.getTreeUtils().getTree(mostEnclosing);
        createWrappersForClass(mostEnclosingTree, javaParserNode, wrapper);
    }

    /**
     * Reads in the file at {@code path} and creates and stores a wrapper around its compilation
     * unit.
     *
     * @param path path to source file to read
     */
    private void addSourceFile(String path) {
        if (sourceFiles.containsKey(path)) {
            return;
        }

        try {
            CompilationUnit root = StaticJavaParser.parse(new File(path));
            new StringLiteralCombineVisitor().visit(root, null);
            CompilationUnitWrapper wrapper = new CompilationUnitWrapper(root);
            sourceFiles.put(path, wrapper);
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
     * @param wrapper compilation unit wrapper to add new wrappers to
     */
    private void createWrappersForClass(
            ClassTree javacClass,
            TypeDeclaration<?> javaParserClass,
            CompilationUnitWrapper wrapper) {
        JointJavacJavaParserVisitor visitor =
                new DefaultJointVisitor(JointJavacJavaParserVisitor.TraversalType.PRE_ORDER) {
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
                        ClassOrInterfaceWrapper typeWrapper = new ClassOrInterfaceWrapper();
                        if (!classes.containsKey(className)) {
                            classes.put(className, typeWrapper);
                        }

                        wrapper.types.add(typeWrapper);
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
                        ClassOrInterfaceWrapper enclosingClass = classes.get(className);
                        String executableName = JVMNames.getJVMMethodSignature(javacTree);
                        if (!enclosingClass.callableDeclarations.containsKey(executableName)) {
                            enclosingClass.callableDeclarations.put(
                                    executableName, new CallableDeclarationWrapper(javaParserNode));
                        }
                    }

                    @Override
                    public void processVariable(
                            VariableTree javacTree, VariableDeclarator javaParserNode) {
                        // This seems to occur when javacTree is a local variable in the second
                        // class located in a source file. If this check return false, then the
                        // below call to TreeUtils.elementFromDeclaration causes a crash.
                        if (TreeUtils.elementFromTree(javacTree) == null) {
                            return;
                        }

                        VariableElement elt = TreeUtils.elementFromDeclaration(javacTree);
                        if (!elt.getKind().isField()) {
                            return;
                        }

                        String className = getEnclosingClassName(elt);
                        ClassOrInterfaceWrapper enclosingClass = classes.get(className);
                        String fieldName = javacTree.getName().toString();
                        if (!enclosingClass.fields.containsKey(fieldName)) {
                            enclosingClass.fields.put(fieldName, new FieldWrapper(javaParserNode));
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

        TypeElement mostEnclosing = mostEnclosingClass(element);
        String path = AjavaUtils.getSourceFilePath(mostEnclosing);
        if (classes.containsKey(getClassName(mostEnclosing))) {
            return path;
        }

        addSourceFile(path);
        CompilationUnitWrapper wrapper = sourceFiles.get(path);
        ClassTree mostEnclosingTree =
                (ClassTree) atypeFactory.declarationFromElement(mostEnclosing);
        TypeDeclaration<?> javaParserNode =
                wrapper.getClassOrInterfaceDeclarationByName(
                        mostEnclosing.getSimpleName().toString());
        createWrappersForClass(mostEnclosingTree, javaParserNode, wrapper);
        return path;
    }

    /**
     * Returns the binary name of the type declaration in {@code element}
     *
     * @param element Element of a type declaration
     * @return the binary name of {@code element}
     */
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    private @BinaryName String getClassName(Element element) {
        return ((ClassSymbol) element).flatName().toString();
    }

    /**
     * Returns outermost class containing {@code element}.
     *
     * @param element element to find enclosing class of
     * @return an element for a class containing {@code element} that isn't contained in another
     *     class
     */
    private TypeElement mostEnclosingClass(Element element) {
        if (ElementUtils.enclosingClass(element) == null) {
            return (TypeElement) element;
        }

        TypeElement mostEnclosing = ElementUtils.enclosingClass(element);
        while (ElementUtils.enclosingClass(mostEnclosing) != null
                && !ElementUtils.enclosingClass(mostEnclosing).equals(mostEnclosing)) {
            mostEnclosing = ElementUtils.enclosingClass(mostEnclosing);
        }

        return mostEnclosing;
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
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    private @BinaryName String getEnclosingClassName(LocalVariableNode localVariableNode) {
        return ((ClassSymbol) ElementUtils.enclosingClass(localVariableNode.getElement()))
                .flatName()
                .toString();
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
     * Adds an explicit receiver type to a JavaParser method declaration with the same type as the
     * surrounding class.
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
                    // TODO: Should this be a ClassOrInterfaceType?
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
     * Stores the JavaParser node for a compilation unit and the list of wrappers for the classes
     * and interfaces in that compilation unit.
     */
    private static class CompilationUnitWrapper {
        /** Compilation unit being wrapped */
        public CompilationUnit declaration;
        /** Wrappers for classes and interfaces in {@code declaration} */
        public List<ClassOrInterfaceWrapper> types;

        /**
         * Constructs a wrapper around the given declaration
         *
         * @param declaration compilation unit to wrap
         */
        public CompilationUnitWrapper(CompilationUnit declaration) {
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
            for (ClassOrInterfaceWrapper type : types) {
                type.transferAnnotations();
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
    private static class ClassOrInterfaceWrapper {
        /**
         * Mapping from JVM method signatures to the wrapper containing the corresponding
         * executable.
         */
        public Map<String, CallableDeclarationWrapper> callableDeclarations;
        /** Mapping from field names to wrappers for those fields. */
        public Map<String, FieldWrapper> fields;

        /** Creates an empty class or interface wrapper. */
        public ClassOrInterfaceWrapper() {
            callableDeclarations = new HashMap<>();
            fields = new HashMap<>();
        }

        /**
         * Transfers all annotations inferred by whole program inference for the methods and fields
         * in the wrapper class or interface to their corresponding JavaParser locations.
         */
        public void transferAnnotations() {
            for (CallableDeclarationWrapper callable : callableDeclarations.values()) {
                callable.transferAnnotations();
            }

            for (FieldWrapper field : fields.values()) {
                field.transferAnnotations();
            }
        }

        @Override
        public String toString() {
            return "ClassOrInterfaceWrapper [callableDeclarations="
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
    private static class CallableDeclarationWrapper {
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
         * Creates a wrapper for the given method or constructor declaration.
         *
         * @param declaration method or constructor declaration to wrap
         */
        public CallableDeclarationWrapper(CallableDeclaration<?> declaration) {
            this.declaration = declaration;
            this.returnType = null;
            this.receiverType = null;
            this.parameterTypes = null;
            this.declarationAnnotations = null;
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
                AnnotatedTypeMirror type, AnnotatedTypeFactory atf, int index) {
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
         * Transfers all annotations inferred by whole program inference for the return type,
         * receiver type, and parameter types for the wrapped declaration to their corresponding
         * JavaParser locations.
         */
        public void transferAnnotations() {
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
            return "CallableDeclarationWrapper [declaration="
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
    private static class FieldWrapper {
        /** Wrapped field declaration. */
        public VariableDeclarator declaration;
        /** Inferred type for field, initialized the first time it's accessed. */
        public @Nullable AnnotatedTypeMirror type;

        /**
         * Creates a wrapper for the given field declaration.
         *
         * @param declaration methofield declaration to wrap
         */
        public FieldWrapper(VariableDeclarator declaration) {
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
            return "FieldWrapper [declaration=" + declaration + ", type=" + type + "]";
        }
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
}
