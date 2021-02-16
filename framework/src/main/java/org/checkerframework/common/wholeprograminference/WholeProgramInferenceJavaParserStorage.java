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
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.framework.ajava.AnnotationConversion;
import org.checkerframework.framework.ajava.AnnotationTransferVisitor;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.ajava.JavaParserUtils;
import org.checkerframework.framework.ajava.JointJavacJavaParserVisitor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import scenelib.annotations.util.JVMNames;

/**
 * This is an implementation of {@link WholeProgramInferenceStorage} that stores annotations
 * directly with the JavaParser node corresponding to the annotation's location. It outputs ajava
 * files.
 */
public class WholeProgramInferenceJavaParserStorage
        implements WholeProgramInferenceStorage<AnnotatedTypeMirror> {

    /** The type factory associated with this. */
    protected final AnnotatedTypeFactory atypeFactory;

    /**
     * Directory where .ajava files will be written to and read from. This directory is relative to
     * where the javac command is executed.
     */
    public static final String AJAVA_FILES_PATH =
            "build" + File.separator + "whole-program-inference" + File.separator;

    /**
     * Maps from binary class name to the wrapper containing the class. Contains all classes in Java
     * source files containing an Element for which an annotation has been inferred.
     */
    private Map<@BinaryName String, ClassOrInterfaceAnnos> classToAnnos;

    /**
     * Files containing classes for which an annotation has been inferred since the last time files
     * were written to disk.
     */
    private Set<String> modifiedFiles;

    /** Mapping from source file to the wrapper for the compilation unit parsed from that file. */
    private Map<String, CompilationUnitAnnos> sourceToAnnos;

    /**
     * Constructs a new {@code WholeProgramInferenceJavaParser} that has not yet inferred any
     * annotations.
     *
     * @param atypeFactory the associated type factory
     */
    public WholeProgramInferenceJavaParserStorage(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
        classToAnnos = new HashMap<>();
        modifiedFiles = new HashSet<>();
        sourceToAnnos = new HashMap<>();
    }

    @Override
    public String getFileForElement(Element elt) {
        return addClassesForElement(elt);
    }

    @Override
    public void setFileModified(String path) {
        modifiedFiles.add(path);
    }

    ///
    /// Reading stored annotations
    ///

    @Override
    public boolean hasStorageLocationForMethod(ExecutableElement methodElt) {
        return getMethodAnnos(methodElt) != null;
    }

    /**
     * Get the annotations for a method or constructor.
     *
     * @param methodElt the method or constructor
     * @return the annotations for a method or constructor
     */
    private CallableDeclarationAnnos getMethodAnnos(ExecutableElement methodElt) {
        String className = ElementUtils.getEnclosingClassName(methodElt);
        // Read in classes for the element.
        getFileForElement(methodElt);
        ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
        CallableDeclarationAnnos methodAnnos =
                classAnnos.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
        return methodAnnos;
    }

    @Override
    public AnnotatedTypeMirror getParameterAnnotations(
            ExecutableElement methodElt,
            int i,
            AnnotatedTypeMirror paramATM,
            VariableElement ve,
            AnnotatedTypeFactory atypeFactory) {
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
        return methodAnnos.getParameterType(paramATM, i, atypeFactory);
    }

    @Override
    public AnnotatedTypeMirror getReceiverAnnotations(
            ExecutableElement methodElt,
            AnnotatedTypeMirror paramATM,
            AnnotatedTypeFactory atypeFactory) {
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
        return methodAnnos.getReceiverType(paramATM, atypeFactory);
    }

    @Override
    public AnnotatedTypeMirror getReturnAnnotations(
            ExecutableElement methodElt,
            AnnotatedTypeMirror atm,
            AnnotatedTypeFactory atypeFactory) {
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
        return methodAnnos.getReturnType(atm, atypeFactory);
    }

    @Override
    public AnnotatedTypeMirror getFieldAnnotations(
            Element element,
            String fieldName,
            AnnotatedTypeMirror lhsATM,
            AnnotatedTypeFactory atypeFactory) {
        ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();
        // Read in classes for the element.
        getFileForElement(element);
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
        return classAnnos.fields.get(fieldName).getType(lhsATM, atypeFactory);
    }

    @Override
    public AnnotatedTypeMirror getPreOrPostconditionsForField(
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
    private AnnotatedTypeMirror getPreconditionsForField(
            ExecutableElement methodElement,
            VariableElement fieldElement,
            AnnotatedTypeFactory atypeFactory) {
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElement);
        return methodAnnos.getPreconditionsForField(fieldElement, atypeFactory);
    }

    /**
     * Returns the postcondition annotations for a field.
     *
     * @param methodElement the method
     * @param fieldElement the field
     * @param atypeFactory the type factory
     * @return the postcondition annotations for a field
     */
    private AnnotatedTypeMirror getPostconditionsForField(
            ExecutableElement methodElement,
            VariableElement fieldElement,
            AnnotatedTypeFactory atypeFactory) {
        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElement);
        return methodAnnos.getPostconditionsForField(fieldElement, atypeFactory);
    }

    @Override
    public boolean addMethodDeclarationAnnotation(
            ExecutableElement methodElt, AnnotationMirror anno) {

        CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
        if (methodAnnos.declarationAnnotations == null) {
            methodAnnos.declarationAnnotations = new LinkedHashSet<AnnotationMirror>();
        }

        boolean isNewAnnotation = methodAnnos.declarationAnnotations.add(anno);
        if (isNewAnnotation) {
            modifiedFiles.add(getFileForElement(methodElt));
        }
        return isNewAnnotation;
    }

    @Override
    public AnnotatedTypeMirror atmFromStorageLocation(
            TypeMirror typeMirror, AnnotatedTypeMirror storageLocation) {
        return storageLocation;
    }

    @Override
    public void updateStorageLocationFromAtm(
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
            updateStorageLocationFromAtm(
                    newAAT.getComponentType(),
                    oldAAT.getComponentType(),
                    aatToUpdate.getComponentType(),
                    defLoc,
                    ignoreIfAnnotated);
        }
    }

    ///
    /// Reading in files
    ///

    @Override
    public void preprocessClassTree(ClassTree classTree) {
        addClassTree(classTree);
    }

    /**
     * Reads in the source file containing {@code tree} and creates a wrapper around {@code tree}.
     *
     * @param tree tree for class to add
     */
    private void addClassTree(ClassTree tree) {
        TypeElement element = TreeUtils.elementFromDeclaration(tree);
        String className = ElementUtils.getBinaryName(element);
        if (classToAnnos.containsKey(className)) {
            return;
        }

        TypeElement toplevelClass = toplevelEnclosingClass(element);
        String path = ElementUtils.getSourceFilePath(toplevelClass);
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
            JavaParserUtils.concatenateAddedStringLiterals(root);
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
                        String className = ElementUtils.getBinaryName(classElt);
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
                        String className = ElementUtils.getEnclosingClassName(elt);
                        ClassOrInterfaceAnnos enclosingClass = classToAnnos.get(className);
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

                        String className = ElementUtils.getEnclosingClassName(elt);
                        ClassOrInterfaceAnnos enclosingClass = classToAnnos.get(className);
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
        String path = ElementUtils.getSourceFilePath(toplevelClass);
        if (classToAnnos.containsKey(ElementUtils.getBinaryName(toplevelClass))) {
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
     * Returns the top-level class that contains {@code element}.
     *
     * @param element the element whose enclosing class to find
     * @return an element for a class containing {@code element} that isn't contained in another
     *     class
     */
    private static TypeElement toplevelEnclosingClass(Element element) {
        TypeElement result = ElementUtils.enclosingTypeElement(element);
        if (result == null) {
            return (TypeElement) element;
        }

        TypeElement enclosing = ElementUtils.strictEnclosingTypeElement(result);
        while (enclosing != null) {
            result = enclosing;
            enclosing = ElementUtils.strictEnclosingTypeElement(enclosing);
        }

        return result;
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
            prepareMethodForWriting(methodEntry.getValue());
        }
    }

    /**
     * Side-effects the method or constructor annotations to make any desired changes before writing
     * to a file.
     *
     * @param methodAnnos the method or constructor annotations to modify
     */
    public void prepareMethodForWriting(CallableDeclarationAnnos methodAnnos) {
        atypeFactory.prepareMethodForWriting(methodAnnos);
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
     * @param target the JavaParser type to transfer annotation to, must represent the same type as
     *     {@code annotatedType}
     */
    private static void transferAnnotations(
            @Nullable AnnotatedTypeMirror annotatedType, Type target) {
        if (annotatedType == null) {
            return;
        }

        target.accept(new AnnotationTransferVisitor(), annotatedType);
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
            JavaParserUtils.clearAnnotations(declaration);
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
            return JavaParserUtils.getTypeDeclarationByName(declaration, name);
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
        private @MonotonicNonNull AnnotatedTypeMirror returnType;
        /**
         * Inferred annotations for the receiver type, if the declaration represents a method.
         * Initialized on first usage.
         */
        private @MonotonicNonNull AnnotatedTypeMirror receiverType;
        /**
         * Inferred annotations for parameter types. Initialized the first time any parameter is
         * accessed and each parameter is initialized the first time it's accessed.
         */
        public @MonotonicNonNull List<@Nullable AnnotatedTypeMirror> parameterTypes;
        /** Annotations on the callable declaration. */
        public @MonotonicNonNull Set<AnnotationMirror> declarationAnnotations;

        /**
         * Mapping from VariableElements for fields to an AnnotatedTypeMirror containing the
         * inferred preconditions on that field.
         */
        public @MonotonicNonNull Map<VariableElement, AnnotatedTypeMirror> fieldToPreconditions;
        /**
         * Mapping from VariableElements for fields to an AnnotatedTypeMirror containing the
         * inferred postconditions on that field.
         */
        public @MonotonicNonNull Map<VariableElement, AnnotatedTypeMirror> fieldToPostconditions;
        // /** Inferred contracts for the callable declaration. */
        // private @MonotonicNonNull List<AnnotationMirror> contracts;

        /**
         * Creates a wrapper for the given method or constructor declaration.
         *
         * @param declaration method or constructor declaration to wrap
         */
        public CallableDeclarationAnnos(CallableDeclaration<?> declaration) {
            this.declaration = declaration;
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
         * If this wrapper holds a method, returns the inferred type of the receiver. If necessary,
         * initializes the {@code AnnotatedTypeMirror} for that location using {@code type} and
         * {@code atf} to a wrapper around the base type for the receiver type.
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
         *
         * @param field VariableElement for a field in the enclosing class for this method
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
         *     preconditions for the given field
         */
        public AnnotatedTypeMirror getPreconditionsForField(
                VariableElement field, AnnotatedTypeFactory atf) {
            if (fieldToPreconditions == null) {
                fieldToPreconditions = new HashMap<>();
            }

            if (!fieldToPreconditions.containsKey(field)) {
                TypeMirror underlyingType = atf.getAnnotatedType(field).getUnderlyingType();
                AnnotatedTypeMirror preconditionsType =
                        AnnotatedTypeMirror.createType(underlyingType, atf, false);
                fieldToPreconditions.put(field, preconditionsType);
            }

            return fieldToPreconditions.get(field);
        }

        /**
         * Returns an AnnotatedTypeMirror containing the postconditions for the given field.
         *
         * @param field VariableElement for a field in the enclosing class for this method
         * @param atf the annotated type factory of a given type system, whose type hierarchy will
         *     be used
         * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
         *     postconditions for the given field
         */
        public AnnotatedTypeMirror getPostconditionsForField(
                VariableElement field, AnnotatedTypeFactory atf) {
            if (fieldToPostconditions == null) {
                fieldToPostconditions = new HashMap<>();
            }

            if (!fieldToPostconditions.containsKey(field)) {
                TypeMirror underlyingType = atf.getAnnotatedType(field).getUnderlyingType();
                AnnotatedTypeMirror postconditionsType =
                        AnnotatedTypeMirror.createType(underlyingType, atf, false);
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
                WholeProgramInferenceJavaParserStorage.transferAnnotations(
                        returnType, declaration.asMethodDeclaration().getType());
            }

            if (receiverType != null) {
                addExplicitReceiver(declaration.asMethodDeclaration());
                // The receiver won't be present for an anonymous class.
                if (declaration.getReceiverParameter().isPresent()) {
                    WholeProgramInferenceJavaParserStorage.transferAnnotations(
                            receiverType, declaration.getReceiverParameter().get().getType());
                }
            }

            if (parameterTypes == null) {
                return;
            }

            for (int i = 0; i < parameterTypes.size(); i++) {
                WholeProgramInferenceJavaParserStorage.transferAnnotations(
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
        private @MonotonicNonNull AnnotatedTypeMirror type = null;

        /**
         * Creates a wrapper for the given field declaration.
         *
         * @param declaration field declaration to wrap
         */
        public FieldAnnos(VariableDeclarator declaration) {
            this.declaration = declaration;
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
            WholeProgramInferenceJavaParserStorage.transferAnnotations(type, newType);
            declaration.setType(newType);
        }

        @Override
        public String toString() {
            return "FieldAnnos [declaration=" + declaration + ", type=" + type + "]";
        }
    }
}
