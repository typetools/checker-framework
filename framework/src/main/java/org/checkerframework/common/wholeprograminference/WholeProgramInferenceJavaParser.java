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
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.ajava.JointJavacJavaParserVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
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
    private Map<String, List<CompilationUnit>> sourceFiles;

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
        // TODO Auto-generated method stub

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

        addSourceFileForElement(methodElt);
    }

    @Override
    public void updateFromOverride(
            MethodTree methodTree,
            ExecutableElement methodElt,
            AnnotatedExecutableType overriddenMethod,
            AnnotatedTypeFactory atf) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateFromLocalAssignment(
            LocalVariableNode lhs,
            Node rhs,
            ClassTree classTree,
            MethodTree methodTree,
            AnnotatedTypeFactory atf) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateFromFieldAssignment(
            Node field, Node rhs, ClassTree classTree, AnnotatedTypeFactory atf) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateFromReturn(
            ReturnNode retNode,
            ClassSymbol classSymbol,
            MethodTree methodTree,
            Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods,
            AnnotatedTypeFactory atf) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeResultsToFile(OutputFormat format, BaseTypeChecker checker) {
        System.out.println("In write results to file");
        System.out.println("Modified files: " + modifiedFiles);
        System.out.println("source files: " + sourceFiles);
        File outputDir = new File(AJAVA_FILES_PATH);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (String path : modifiedFiles) {
            for (CompilationUnit root : sourceFiles.get(path)) {
                String packageDir = AJAVA_FILES_PATH;
                if (root.getPackageDeclaration().isPresent()) {
                    packageDir +=
                            File.separator
                                    + root.getPackageDeclaration()
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
                    System.out.println("About to print root: " + root);
                    LexicalPreservingPrinter.print(root, writer);
                    writer.close();
                } catch (IOException e) {
                    throw new BugInCF("Error while writing ajava file", e);
                }
            }
        }
    }

    private void addSourceFile(String path) {
        System.out.println("Adding source file: " + path);
        if (sourceFiles.containsKey(path)) {
            return;
        }

        List<CompilationUnit> fileRoots = new ArrayList<>();

        try {
            Source source = new Source(path);
            System.out.println("About to parse source files with javac");
            Set<CompilationUnitTree> compilationUnits = source.parse();
            for (CompilationUnitTree root : compilationUnits) {
                CompilationUnit javaParserRoot = addCompilationUnit(root, path);
                fileRoots.add(javaParserRoot);
            }

            sourceFiles.put(path, fileRoots);
            modifiedFiles.add(path);
        } catch (CompilerException | IOException e) {
            throw new BugInCF("Failed to read java file: " + path, e);
        }
    }

    private CompilationUnit addCompilationUnit(CompilationUnitTree root, String path)
            throws IOException {
        CompilationUnit javaParserRoot =
                StaticJavaParser.parse(root.getSourceFile().openInputStream());
        System.out.println("Seting up root: " + javaParserRoot);
        LexicalPreservingPrinter.setup(javaParserRoot);
        JointJavacJavaParserVisitor visitor =
                new DefaultJointVisitor(JointJavacJavaParserVisitor.TraversalType.PRE_ORDER) {
                    @Override
                    public void processClass(
                            ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode) {
                        TypeElement classElt = TreeUtils.elementFromDeclaration(javacTree);
                        String className = getClassName(classElt);
                        if (!classes.containsKey(className)) {
                            classes.put(className, new ClassOrInterfaceWrapper(javaParserNode));
                        }
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
                        if (!enclosingClass.methods.containsKey(executableName)) {
                            enclosingClass.methods.put(
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
        return javaParserRoot;
    }

    private void addSourceFileForElement(Element element) {
        if (!ElementUtils.isElementFromSourceCode(element)) {
            throw new BugInCF("Adding source file for non-source element: " + element);
        }

        if (!(element instanceof ClassSymbol)) {
            addSourceFileForElement(element.getEnclosingElement());
            return;
        }

        ClassSymbol symbol = (ClassSymbol) element;
        addSourceFile(symbol.sourcefile.toUri().getPath());
    }

    private @BinaryName String getClassName(Element element) {
        return ((ClassSymbol) element).flatName().toString();
    }

    private @BinaryName String getEnclosingClassName(ExecutableElement executableElement) {
        return ((MethodSymbol) executableElement).enclClass().flatName().toString();
    }

    private @BinaryName String getEnclosingClassName(VariableElement variableElement) {
        return getClassName(ElementUtils.enclosingClass(variableElement));
    }

    private static class ClassOrInterfaceWrapper {
        public String file;
        public ClassOrInterfaceDeclaration declaration;
        public Map<String, CallableDeclarationWrapper> methods;
        public Map<String, FieldWrapper> fields;

        public ClassOrInterfaceWrapper(ClassOrInterfaceDeclaration declaration) {
            this.declaration = declaration;
            methods = new HashMap<>();
        }
    }

    private static class CallableDeclarationWrapper {
        public CallableDeclaration<?> declaration;
        public String file;
        public @Nullable AnnotatedTypeMirror returnType;
        public @Nullable AnnotatedTypeMirror receiverType;
        public List<@Nullable AnnotatedTypeMirror> parameterTypes;

        public CallableDeclarationWrapper(CallableDeclaration<?> declaration) {
            this.declaration = declaration;
            this.returnType = null;
            this.receiverType = null;
            this.parameterTypes = new ArrayList<>();
        }
    }

    private static class FieldWrapper {
        public VariableDeclarator declaration;
        public AnnotatedTypeMirror type;

        public FieldWrapper(VariableDeclarator declaration) {
            this.declaration = declaration;
        }
    }
}
