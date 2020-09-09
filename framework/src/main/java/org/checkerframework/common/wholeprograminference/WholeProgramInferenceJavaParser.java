package org.checkerframework.common.wholeprograminference;

import annotator.Source;
import annotator.Source.CompilerException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.checkerframework.framework.ajava.AnnotationTransferVisitor;
import org.checkerframework.framework.ajava.ClearAnnotationsVisitor;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.ajava.JointJavacJavaParserVisitor;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import scenelib.annotations.util.JVMNames;

@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
public class WholeProgramInferenceJavaParser implements WholeProgramInference {
    /**
     * Directory where .ajava files will be written to and read from. This directory is relative to
     * where the CF's javac command is executed.
     */
    public static final String AJAVA_FILES_PATH =
            "build" + File.separator + "whole-program-inference" + File.separator;

    private Map<@BinaryName String, ClassOrInterfaceWrapper> classes;
    private Set<String> modifiedFiles;
    private Map<String, List<CompilationUnitWrapper>> sourceFiles;

    public WholeProgramInferenceJavaParser() {
        classes = new HashMap<>();
        modifiedFiles = new HashSet<>();
        sourceFiles = new HashMap<>();
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

        String file = addSourceFileForElement(constructorElt);
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

        String file = addSourceFileForElement(methodElt);
        String className = getEnclosingClassName(methodElt);
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));

        List<Node> arguments = methodInvNode.getArguments();
        updateInferredExecutableParameterTypes(methodElt, atf, file, method, arguments);
    }

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

            updateAnnotationSetInScene(
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

        String file = addSourceFileForElement(methodElt);
        String className = getEnclosingClassName(methodElt);
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(ve);

            AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
            AnnotatedTypeMirror param = method.getParameterType(argATM, atf, i);
            updateAnnotationSetInScene(
                    param, atf, file, argATM, paramATM, TypeUseLocation.PARAMETER);
        }

        AnnotatedDeclaredType argADT = overriddenMethod.getReceiverType();
        if (argADT != null) {
            AnnotatedTypeMirror paramATM = atf.getAnnotatedType(methodTree).getReceiverType();
            if (paramATM != null) {
                AnnotatedTypeMirror receiver = method.getReceiverType(paramATM, atf);
                updateAnnotationSetInScene(
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
        String file = addSourceFileForElement(methodElt);
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
                updateAnnotationSetInScene(
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

        String file = addSourceFileForElement(element);
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        ClassOrInterfaceWrapper clazz = classes.get(className);

        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(lhs.getTree());
        AnnotatedTypeMirror field = clazz.fields.get(fieldName).getType(lhsATM, atf);
        // TODO: For a primitive such as long, this is yielding just @GuardedBy rather than
        // @GuardedBy({}).
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(rhs.getTree());
        updateAnnotationSetInScene(field, atf, file, rhsATM, lhsATM, TypeUseLocation.FIELD);
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
        String file = addSourceFileForElement(methodElt);
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = classSymbol.flatname.toString();
        ClassOrInterfaceWrapper clazz = classes.get(className);
        CallableDeclarationWrapper method =
                clazz.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        AnnotatedTypeMirror lhsATM = atf.getAnnotatedType(methodTree).getReturnType();

        // Type of the expression returned
        AnnotatedTypeMirror rhsATM = atf.getAnnotatedType(retNode.getTree().getExpression());
        AnnotatedTypeMirror returnType = method.getReturnType(lhsATM, atf);
        updateAnnotationSetInScene(returnType, atf, file, rhsATM, lhsATM, TypeUseLocation.RETURN);

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

            // TODO: is it superClass or superclass?
            String superClassFile = addSourceFileForElement(overriddenMethodElement);
            String superClassName = getEnclosingClassName(overriddenMethodElement);
            ClassOrInterfaceWrapper superClazz = classes.get(superClassName);
            CallableDeclarationWrapper overriddenMethodInSuperclass =
                    superClazz.callableDeclarations.get(
                            JVMNames.getJVMMethodSignature(overriddenMethodElement));
            AnnotatedTypeMirror overriddenMethodReturnType = overriddenMethod.getReturnType();
            AnnotatedTypeMirror storedOverriddenMethodReturnType =
                    overriddenMethodInSuperclass.getReturnType(overriddenMethodReturnType, atf);

            updateAnnotationSetInScene(
                    storedOverriddenMethodReturnType,
                    atf,
                    superClassFile,
                    rhsATM,
                    overriddenMethodReturnType,
                    TypeUseLocation.RETURN);
        }
    }

    @Override
    public void writeResultsToFile(OutputFormat format, BaseTypeChecker checker) {
        File outputDir = new File(AJAVA_FILES_PATH);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (String path : modifiedFiles) {
            for (CompilationUnitWrapper root : sourceFiles.get(path)) {
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

                name += ".ajava";
                String outputPath = packageDir + File.separator + name;
                try {
                    FileWriter writer = new FileWriter(outputPath);
                    PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
                    configuration.setIndentType(PrettyPrinterConfiguration.IndentType.SPACES);
                    configuration.setIndentSize(4);
                    configuration.setPrintComments(false);
                    PrettyPrinter prettyPrinter = new PrettyPrinter(configuration);
                    writer.write(prettyPrinter.print(root.declaration));
                    writer.close();
                } catch (IOException e) {
                    throw new BugInCF("Error while writing ajava file", e);
                }
            }
        }
    }

    protected void updateAnnotationSetInScene(
            AnnotatedTypeMirror type,
            AnnotatedTypeFactory atf,
            String file,
            AnnotatedTypeMirror rhsATM,
            AnnotatedTypeMirror lhsATM,
            TypeUseLocation defLoc) {
        // TODO: Ignore null types here? See corresponding place in
        // WholeProgramInferenceScenesStorage
        AnnotatedTypeMirror atmFromJaif =
                AnnotatedTypeMirror.createType(rhsATM.getUnderlyingType(), atf, false);
        updatesATMWithLUB(atf, rhsATM, atmFromJaif);
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

        updateTypeElementFromATM(rhsATM, lhsATM, atf, type, defLoc);
        modifiedFiles.add(file);
    }

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
    private void updatesATMWithLUB(
            AnnotatedTypeFactory atf,
            AnnotatedTypeMirror sourceCodeATM,
            AnnotatedTypeMirror jaifATM) {

        switch (sourceCodeATM.getKind()) {
            case TYPEVAR:
                updatesATMWithLUB(
                        atf,
                        ((AnnotatedTypeVariable) sourceCodeATM).getLowerBound(),
                        ((AnnotatedTypeVariable) jaifATM).getLowerBound());
                updatesATMWithLUB(
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
                updatesATMWithLUB(
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

    private void addSourceFile(String path) {
        if (sourceFiles.containsKey(path)) {
            return;
        }

        List<CompilationUnitWrapper> fileRoots = new ArrayList<>();

        try {
            Source source = new Source(path);
            Set<CompilationUnitTree> compilationUnits = source.parse();
            for (CompilationUnitTree root : compilationUnits) {
                CompilationUnitWrapper javaParserRoot = addCompilationUnit(root, path);
                fileRoots.add(javaParserRoot);
            }

            sourceFiles.put(path, fileRoots);
            modifiedFiles.add(path);
        } catch (CompilerException | IOException e) {
            throw new BugInCF("Failed to read java file: " + path, e);
        }
    }

    private CompilationUnitWrapper addCompilationUnit(CompilationUnitTree root, String path)
            throws IOException {
        CompilationUnit javaParserRoot =
                StaticJavaParser.parse(root.getSourceFile().openInputStream());
        CompilationUnitWrapper wrapper = new CompilationUnitWrapper(javaParserRoot);
        JointJavacJavaParserVisitor visitor =
                new DefaultJointVisitor(JointJavacJavaParserVisitor.TraversalType.PRE_ORDER) {
                    @Override
                    public void processClass(
                            ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode) {
                        TypeElement classElt = TreeUtils.elementFromDeclaration(javacTree);
                        String className = getClassName(classElt);
                        ClassOrInterfaceWrapper typeWrapper =
                                new ClassOrInterfaceWrapper(javaParserNode);
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
        visitor.visitCompilationUnit(root, javaParserRoot);
        return wrapper;
    }

    private String addSourceFileForElement(Element element) {
        if (!ElementUtils.isElementFromSourceCode(element)) {
            throw new BugInCF("Adding source file for non-source element: " + element);
        }

        if (!(element instanceof ClassSymbol)) {
            return addSourceFileForElement(element.getEnclosingElement());
        }

        ClassSymbol symbol = (ClassSymbol) element;
        String path = symbol.sourcefile.toUri().getPath();
        addSourceFile(path);
        return path;
    }

    private @BinaryName String getClassName(Element element) {
        return ((ClassSymbol) element).flatName().toString();
    }

    /**
     * Returns the "flatname" of the class enclosing {@code executableElement}
     *
     * @param executableElement the ExecutableElement
     * @return the "flatname" of the class enclosing {@code executableElement}
     */
    private @BinaryName String getEnclosingClassName(ExecutableElement executableElement) {
        return ((MethodSymbol) executableElement).enclClass().flatName().toString();
    }

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

    private static class CompilationUnitWrapper {
        public CompilationUnit declaration;
        public List<ClassOrInterfaceWrapper> types;

        public CompilationUnitWrapper(CompilationUnit declaration) {
            this.declaration = declaration;
            types = new ArrayList<>();
        }

        public void transferAnnotations() {
            ClearAnnotationsVisitor annotationClearer = new ClearAnnotationsVisitor();
            declaration.accept(annotationClearer, null);
            for (ClassOrInterfaceWrapper type : types) {
                type.transferAnnotations();
            }
        }
    }

    private static class ClassOrInterfaceWrapper {
        public ClassOrInterfaceDeclaration declaration;
        public Map<String, CallableDeclarationWrapper> callableDeclarations;
        public Map<String, FieldWrapper> fields;

        public ClassOrInterfaceWrapper(ClassOrInterfaceDeclaration declaration) {
            this.declaration = declaration;
            callableDeclarations = new HashMap<>();
            fields = new HashMap<>();
        }

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
            return "ClassOrInterfaceWrapper [declaration="
                    + declaration
                    + ", fields="
                    + fields
                    + ", callableDeclarations="
                    + callableDeclarations
                    + "]";
        }
    }

    private static class CallableDeclarationWrapper {
        public CallableDeclaration<?> declaration;
        public String file;
        // TODO: Make these private?
        public @Nullable AnnotatedTypeMirror returnType;
        public @Nullable AnnotatedTypeMirror receiverType;
        public @Nullable List<@Nullable AnnotatedTypeMirror> parameterTypes;

        public CallableDeclarationWrapper(CallableDeclaration<?> declaration) {
            this.declaration = declaration;
            this.returnType = null;
            this.receiverType = null;
            this.parameterTypes = null;
        }

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

        public AnnotatedTypeMirror getReturnType(
                AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (returnType == null) {
                returnType = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
            }

            return returnType;
        }

        public AnnotatedTypeMirror getReceiverType(
                AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (receiverType == null) {
                receiverType = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
            }

            return receiverType;
        }

        public void transferAnnotations() {
            if (returnType != null) {
                // If a return type exists, then the declaration must be a method, not a
                // constructor.
                WholeProgramInferenceJavaParser.transferAnnotations(
                        returnType, declaration.asMethodDeclaration().getType());
            }

            if (receiverType != null) {
                WholeProgramInferenceJavaParser.transferAnnotations(
                        receiverType, declaration.getReceiverParameter().get().getType());
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

    private static class FieldWrapper {
        public VariableDeclarator declaration;
        public AnnotatedTypeMirror type;

        public FieldWrapper(VariableDeclarator declaration) {
            this.declaration = declaration;
            type = null;
        }

        public AnnotatedTypeMirror getType(AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
            if (type == null) {
                type = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
            }

            return type;
        }

        public void transferAnnotations() {
            WholeProgramInferenceJavaParser.transferAnnotations(type, declaration.getType());
        }

        @Override
        public String toString() {
            return "FieldWrapper [declaration=" + declaration + ", type=" + type + "]";
        }
    }

    private static AnnotationExpr makeAnnotationExpr(AnnotationMirror annotation) {
        // String name = AnnotationUtils.annotationName(annotation);
        // Map<? extends ExecutableElement, ? extends AnnotationValue> values =
        // annotation.getElementValues();
        return null;
    }

    private static void transferAnnotations(
            @Nullable AnnotatedTypeMirror annotatedType, Type target) {
        if (annotatedType == null) {
            return;
        }

        target.accept(new AnnotationTransferVisitor(), annotatedType);
    }
}
