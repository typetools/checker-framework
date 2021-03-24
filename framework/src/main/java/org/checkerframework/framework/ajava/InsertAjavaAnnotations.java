package org.checkerframework.framework.ajava;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.utils.Pair;
import com.sun.source.util.JavacTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.javacutil.BugInCF;

/** Inserts annotations from an ajava file into a Java file. */
public class InsertAjavaAnnotations {
    /** Utility class for working with Elements. */
    private Elements elements;

    /**
     * Gets an instance of {@code Elements} from the current Java compiler.
     *
     * @return an instance of {@code Elements}
     */
    private static Elements createElements() {
        JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("Could not get compiler instance");
            System.exit(0);
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        if (fileManager == null) {
            System.err.println("Could not get file manager");
        }

        CompilationTask cTask =
                compiler.getTask(
                        null,
                        fileManager,
                        diagnostics,
                        Collections.emptyList(),
                        null,
                        Collections.emptyList());
        if (!(cTask instanceof JavacTask)) {
            System.err.println("Could not get a valid JavacTask: " + cTask.getClass());
        }

        return ((JavacTask) cTask).getElements();
    }

    /**
     * Constructs an {@code InsertAjavaAnnotations} using the given {@code Elements} instance.
     *
     * @param elements an instance of {@code Elements}
     */
    public InsertAjavaAnnotations(Elements elements) {
        this.elements = elements;
    }

    /** Represents some text to be inserted at a file and its location. */
    private static class Insertion {
        /** Offset of the insertion in characters. */
        public int position;
        /** The contents of the insertion. */
        public String contents;
        /** Whether the insertion represents an object on its own line. */
        public boolean ownLine;

        /**
         * Constructs an insertion with the given position and contents.
         *
         * @param position offset of the insertion in the file
         * @param contents contents of the insertion
         */
        public Insertion(int position, String contents) {
            this(position, contents, false);
        }

        /**
         * Constructs an insertion with the given position and contents.
         *
         * @param position offset of the insertion in the file
         * @param contents contents of the insertion
         * @param ownLine true if this insertion represents an object on its own line (doesn't
         *     affect the contents of the insertion)
         */
        public Insertion(int position, String contents, boolean ownLine) {
            this.position = position;
            this.contents = contents;
            this.ownLine = ownLine;
        }

        @Override
        public String toString() {
            return "Insertion [contents=" + contents + ", position=" + position + "]";
        }
    }

    /**
     * Given two JavaParser ASTs representing the same Java file but with differing annotations,
     * stores a list of {@link Insertion}s for all annotations in the first AST into the second AST.
     */
    private class BuildInsertionsVisitor extends DoubleJavaParserVisitor {
        /**
         * The set of annotations found in the file. Keys are both fully-qualified and simple names.
         * There are two entries for each annotation: the annotation's simple name and its
         * fully-qualified name.
         *
         * <p>The map is populated from import statements and also by when parsing a file that uses
         * the fully qualified name of an annotation it doesn't import.
         */
        private Map<String, TypeElement> allAnnotations;

        /** The annotation insertions seen so far. */
        public List<Insertion> insertions;
        /** A printer for annotations. */
        private PrettyPrinter printer;
        /** The lines of the String representation of the second AST. */
        private List<String> lines;
        /**
         * Stores the offsets of the lines in the string representation of the second AST. At index
         * i, stores the number of characters from the start of the file to the beginning of the ith
         * line.
         */
        private List<Integer> cumulativeLineSizes;

        /**
         * Constructs a {@code BuildInsertionsVisitor} where {@code destFileContents} is the String
         * representation of the AST to insertion annotations to. When visiting a node pair, the
         * second node must always be from an AST generated from this String.
         *
         * @param destFileContents the String the second vistide AST was parsed from
         */
        public BuildInsertionsVisitor(String destFileContents) {
            allAnnotations = null;
            insertions = new ArrayList<>();
            printer = new PrettyPrinter();
            String[] lines = destFileContents.split(System.lineSeparator());
            this.lines = Arrays.asList(lines);
            cumulativeLineSizes = new ArrayList<>();
            cumulativeLineSizes.add(0);
            for (int i = 1; i < lines.length; i++) {
                int lastSize = cumulativeLineSizes.get(i - 1);
                int lastLineLength = lines[i - 1].length() + System.lineSeparator().length();
                cumulativeLineSizes.add(lastSize + lastLineLength);
            }
        }

        @Override
        public void defaultAction(Node node1, Node node2) {
            if (!(node1 instanceof NodeWithAnnotations<?>)) {
                return;
            }

            NodeWithAnnotations<?> node1WithAnnos = (NodeWithAnnotations<?>) node1;
            if (node1 instanceof MethodDeclaration) {
                addAnnotationOnOwnLine(node2.getBegin().get(), node1WithAnnos.getAnnotations());
                return;
            } else if (node1 instanceof FieldDeclaration) {
                addAnnotationOnOwnLine(node2.getBegin().get(), node1WithAnnos.getAnnotations());
                return;
            }

            Position position;
            if (node2 instanceof ClassOrInterfaceType) {
                // In a multi-part name like my.package.MyClass, annotations go directly in front of
                // MyClass instead of the full name.
                position = ((ClassOrInterfaceType) node2).getName().getBegin().get();
            } else {
                position = node2.getBegin().get();
            }

            addAnnotations(position, node1WithAnnos.getAnnotations(), 0, false);
        }

        @Override
        public void visit(ArrayType node1, Node other) {
            ArrayType node2 = (ArrayType) other;
            // The second component of this pair contains a list of ArrayBracketPairs from left to
            // right. For example, if node1 contains String[][], then the list will contain the
            // types String[] and String[][]. To insert array annotations in the correct location,
            // we insert them directly to the right of the end of the previous element.
            Pair<Type, List<ArrayType.ArrayBracketPair>> node1ArrayTypes =
                    ArrayType.unwrapArrayTypes(node1);
            Pair<Type, List<ArrayType.ArrayBracketPair>> node2ArrayTypes =
                    ArrayType.unwrapArrayTypes(node2);
            // The first annotations go directly after the element type.
            Position firstPosition = node2ArrayTypes.a.getEnd().get();
            addAnnotations(firstPosition, node1ArrayTypes.b.get(0).getAnnotations(), 1, false);
            for (int i = 1; i < node1ArrayTypes.b.size(); i++) {
                Position position =
                        node2ArrayTypes.b.get(i - 1).getTokenRange().get().toRange().get().end;
                addAnnotations(position, node1ArrayTypes.b.get(i).getAnnotations(), 1, true);
            }

            // Visit the component type.
            node1ArrayTypes.a.accept(this, node2ArrayTypes.a);
        }

        @Override
        public void visit(final CompilationUnit node1, final Node other) {
            CompilationUnit node2 = (CompilationUnit) other;
            defaultAction(node1, node2);

            // Gather annotations used in the ajava file.
            allAnnotations = getAllAnnotations(node1);

            // Move any annotations that appear in the declaration position but belong only in the
            // type position.
            node1.accept(new TypeAnnotationMover(allAnnotations, elements), null);

            // Transfer import statements from the ajava file to the Java file.
            Set<String> existingImports = new HashSet<>();
            for (ImportDeclaration importDecl : node2.getImports()) {
                existingImports.add(printer.print(importDecl));
            }

            List<String> newImports = new ArrayList<>();
            for (ImportDeclaration importDecl : node1.getImports()) {
                String importString = printer.print(importDecl);
                if (!existingImports.contains(importString)) {
                    newImports.add(importString);
                }
            }

            if (!newImports.isEmpty()) {
                int position;
                int lineBreaks;
                if (!node2.getImports().isEmpty()) {
                    Position lastImportPosition =
                            node2.getImports().get(node2.getImports().size() - 1).getEnd().get();
                    position = getAbsolutePosition(lastImportPosition) + 1;
                    lineBreaks = 1;
                } else if (node2.getPackageDeclaration().isPresent()) {
                    Position packagePosition = node2.getPackageDeclaration().get().getEnd().get();
                    position = getAbsolutePosition(packagePosition) + 1;
                    lineBreaks = 2;
                } else {
                    position = 0;
                    lineBreaks = 0;
                }

                String insertionContent = String.join("", newImports);
                for (int i = 0; i < lineBreaks; i++) {
                    insertionContent = System.lineSeparator() + insertionContent;
                }

                insertions.add(new Insertion(position, insertionContent));
            }

            node1.getModule().ifPresent(l -> l.accept(this, node2.getModule().get()));
            node1.getPackageDeclaration()
                    .ifPresent(l -> l.accept(this, node2.getPackageDeclaration().get()));
            for (int i = 0; i < node1.getTypes().size(); i++) {
                node1.getTypes().get(i).accept(this, node2.getTypes().get(i));
            }
        }

        /**
         * Creates an insertion for a collection of annotations. The annotations will appear on
         * their own line unless any non-whitespace characters precede the insertion position on its
         * own line.
         *
         * @param position the position of the insertion
         * @param annotations List of annotations to insert
         */
        private void addAnnotationOnOwnLine(Position position, List<AnnotationExpr> annotations) {
            String line = lines.get(position.line - 1);
            int insertionColumn = position.column - 1;
            boolean ownLine = true;
            for (int i = 0; i < insertionColumn; i++) {
                if (line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                    ownLine = false;
                    break;
                }
            }

            if (ownLine) {
                StringBuilder insertionContent = new StringBuilder();
                for (int i = 0; i < annotations.size(); i++) {
                    insertionContent.append(printer.print(annotations.get(i)));
                    if (i < annotations.size() - 1) {
                        insertionContent.append(" ");
                    }
                }

                if (insertionContent.length() == 0) {
                    return;
                }

                String whitespaceCopy = line.substring(0, position.column - 1);
                insertionContent.append(System.lineSeparator());
                insertionContent.append(whitespaceCopy);
                int absolutePosition = getAbsolutePosition(position);
                insertions.add(new Insertion(absolutePosition, insertionContent.toString(), true));
            } else {
                addAnnotations(position, annotations, 0, false);
            }
        }

        /**
         * Creates an insertion for a collection of annotations at {@code position} + {@code
         * offset}.
         *
         * @param position the position of the insertion
         * @param annotations List of annotations to insert
         * @param offset additional offset of the insertion after {@code position}
         * @param addSpaceBefore if true, the insertion content will start with a space
         */
        private void addAnnotations(
                Position position,
                Iterable<AnnotationExpr> annotations,
                int offset,
                boolean addSpaceBefore) {
            StringBuilder insertionContent = new StringBuilder();
            for (AnnotationExpr annotation : annotations) {
                insertionContent.append(printer.print(annotation));
                insertionContent.append(" ");
            }

            if (insertionContent.length() == 0) {
                return;
            }

            if (addSpaceBefore) {
                insertionContent.insert(0, " ");
            }

            int absolutePosition = getAbsolutePosition(position);
            absolutePosition += offset;
            insertions.add(new Insertion(absolutePosition, insertionContent.toString()));
        }

        /**
         * Converts a Position (which contains a line and column) to an absolute offset from the
         * start of the file
         *
         * @param position a Position
         * @return the total offset of the position from the start of the file
         */
        private int getAbsolutePosition(Position position) {
            return cumulativeLineSizes.get(position.line - 1) + (position.column - 1);
        }
    }

    // TODO: BEGIN from AnnotationFileParser. Factor out of both.
    //
    //
    //
    //
    /**
     * Temp.
     *
     * @return a map from names to TypeElement, for all annotations imported by the annotation file.
     *     Two entries for each annotation: one for the simple name and another for the
     *     fully-qualified name, with the same value.
     * @param cu tmp
     */
    private Map<String, TypeElement> getAllAnnotations(CompilationUnit cu) {
        Map<String, TypeElement> result = new HashMap<>();
        if (cu.getImports() == null) {
            return result;
        }

        for (ImportDeclaration importDecl : cu.getImports()) {
            try {
                if (importDecl.isAsterisk()) {
                    @SuppressWarnings("signature" // https://tinyurl.com/cfissue/3094:
                    // com.github.javaparser.ast.expr.Name inherits toString,
                    // so there can be no annotation for it
                    )
                    @DotSeparatedIdentifiers String imported = importDecl.getName().toString();
                    if (importDecl.isStatic()) {
                        // Wildcard import of members of a type (class or interface)
                        TypeElement element = getTypeElement(imported);
                        if (element != null) {
                            // Find nested annotations
                            // Find compile time constant fields, or values of an enum
                            putAllNew(result, annosInType(element));
                        }

                    } else {
                        // Wildcard import of members of a package
                        PackageElement element = findPackage(imported);
                        if (element != null) {
                            putAllNew(result, annosInPackage(element));
                        }
                    }
                } else {
                    // A single (non-wildcard) import.
                    @SuppressWarnings("signature" // importDecl is non-wildcard, so its name is
                    // @FullyQualifiedName
                    )
                    @FullyQualifiedName String imported = importDecl.getNameAsString();

                    final TypeElement importType = elements.getTypeElement(imported);
                    if (importType == null && !importDecl.isStatic()) {
                        // Class or nested class (according to JSL), but we can't resolve

                        // stubWarnNotFound(importDecl, "type not found: " + imported);
                    } else if (importType == null) {
                        // static import of field or method.
                    } else if (importType.getKind() == ElementKind.ANNOTATION_TYPE) {
                        // Single annotation or nested annotation
                        TypeElement annoElt = elements.getTypeElement(imported);
                        if (annoElt != null) {
                            putIfAbsent(result, annoElt.getSimpleName().toString(), annoElt);
                        } else {
                            // stubWarnNotFound(importDecl, "Could not load import: " + imported);
                        }
                    } else {
                        // Class or nested class
                        // TODO: Is this needed?
                        // importedConstants.add(imported);
                        // TypeElement element =
                        // getTypeElement(imported, "Imported type not found");
                        // importedTypes.put(element.getSimpleName().toString(), element);
                    }
                }
            } catch (AssertionError error) {
                // stubWarnNotFound(importDecl, error.toString());
            }
        }
        return result;
    }

    /**
     * Returns the element for the given package.
     *
     * @param packageName the package's name
     * @return the element for the given package
     */
    private PackageElement findPackage(String packageName) {
        PackageElement packageElement = elements.getPackageElement(packageName);
        // if (packageElement == null) {
        // stubWarnNotFound(astNode, "Imported package not found: " + packageName);
        // }
        return packageElement;
    }

    /**
     * Just like Map.putAll, but modifies existing values using {@link #putIfAbsent(Map, Object,
     * Object)}.
     *
     * @param m the destination map
     * @param m2 the source map
     * @param <K> the key type for the maps
     * @param <V> the value type for the maps
     */
    public static <K, V> void putAllNew(Map<K, V> m, Map<K, V> m2) {
        for (Map.Entry<K, V> e2 : m2.entrySet()) {
            putIfAbsent(m, e2.getKey(), e2.getValue());
        }
    }

    /**
     * Get the type element for the given fully-qualified type name. If none is found, issue a
     * warning and return null.
     *
     * @param typeName a type name
     * @return the type element for the given fully-qualified type name, or null
     */
    private TypeElement getTypeElement(@FullyQualifiedName String typeName) {
        TypeElement classElement = elements.getTypeElement(typeName);
        return classElement;
    }

    /**
     * All annotations defined in the package (but not those nested within classes in the package).
     * Keys are both fully-qualified and simple names.
     *
     * @param packageElement a package
     * @return a map from annotation name to TypeElement
     */
    private Map<String, TypeElement> annosInPackage(PackageElement packageElement) {
        return createNameToAnnotationMap(
                ElementFilter.typesIn(packageElement.getEnclosedElements()));
    }

    /**
     * All annotations declared (directly) within a class. Keys are both fully-qualified and simple
     * names.
     *
     * @param typeElement a type
     * @return a map from annotation name to TypeElement
     */
    private Map<String, TypeElement> annosInType(TypeElement typeElement) {
        return createNameToAnnotationMap(ElementFilter.typesIn(typeElement.getEnclosedElements()));
    }

    /**
     * All annotations declared within any of the given elements.
     *
     * @param typeElements the elements whose annotations to retrieve
     * @return a map from annotation names (both fully-qualified and simple names) to TypeElement
     */
    public static Map<String, TypeElement> createNameToAnnotationMap(
            List<TypeElement> typeElements) {
        Map<String, TypeElement> result = new HashMap<>();
        for (TypeElement typeElm : typeElements) {
            if (typeElm.getKind() == ElementKind.ANNOTATION_TYPE) {
                putIfAbsent(result, typeElm.getSimpleName().toString(), typeElm);
                putIfAbsent(result, typeElm.getQualifiedName().toString(), typeElm);
            }
        }
        return result;
    }

    /**
     * Just like Map.put, but does not override any existing value in the map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m a map
     * @param key a key
     * @param value the value to associate with the key, if the key isn't already in the map
     */
    private static <K, V> void putIfAbsent(Map<K, V> m, K key, V value) {
        if (key == null) {
            throw new BugInCF("AnnotationFileParser: key is null for value " + value);
        }
        if (!m.containsKey(key)) {
            m.put(key, value);
        }
    }
    // TODO: END from AnnotationFileParser

    /**
     * Inserts all annotations from the ajava file read from the stream {@code annotationFile} into
     * a Java file with contents {@code fileContents} and returns the result.
     *
     * @param annotationFile input stream for an ajava file for {@code fileContents}
     * @param fileContents contents of a Java file to insert annotations into
     * @return a modified {@code fileContents} with annotations from {@code annotationFile} inserted
     */
    public String insertAnnotations(InputStream annotationFile, String fileContents) {
        CompilationUnit annotationCu = StaticJavaParser.parse(annotationFile);
        CompilationUnit fileCu = StaticJavaParser.parse(fileContents);
        BuildInsertionsVisitor insertionVisitor = new BuildInsertionsVisitor(fileContents);
        annotationCu.accept(insertionVisitor, fileCu);
        List<Insertion> insertions = insertionVisitor.insertions;
        // Insert annotations in reverse order of position. Making an insertion changes the offset
        // values of everything after the insertion, so making the insertions in reverse order of
        // removes the need to recalculate positions.
        insertions.sort(
                (insertion1, insertion2) -> {
                    int cmp = Integer.compare(insertion1.position, insertion2.position);
                    // Annotations belonging on their own line should be inserted before other
                    // annotations. For example, in
                    //
                    // @Pure
                    // @Tainted String myMethod();
                    //
                    // both annotations should be inserted at the same position (the start of
                    // "String"), but @Pure should always appear first.
                    if (cmp == 0 && (insertion1.ownLine != insertion2.ownLine)) {
                        if (insertion1.ownLine) {
                            cmp = -1;
                        } else {
                            cmp = 1;
                        }
                    }

                    return -cmp;
                });
        StringBuilder result = new StringBuilder(fileContents);
        for (Insertion insertion : insertions) {
            result.insert(insertion.position, insertion.contents);
        }

        return result.toString();
    }

    /**
     * Inserts all annotations from the ajava file at {@code annotationFilePath} to {@code
     * javaFilePath}.
     *
     * @param annotationFilePath path to an ajava file for {@code javaFilePath}
     * @param javaFilePath path to a Java file to insert annotation into
     */
    public void insertAnnotations(String annotationFilePath, String javaFilePath) {
        try {
            Path path = Paths.get(javaFilePath);
            String fileContents = new String(Files.readAllBytes(path));
            FileInputStream annotationInputStream = new FileInputStream(annotationFilePath);
            String result = insertAnnotations(annotationInputStream, fileContents);
            annotationInputStream.close();
            Files.write(path, result.getBytes());
        } catch (IOException e) {
            System.err.println(
                    "Failed to insertion annotations from file "
                            + annotationFilePath
                            + " to file "
                            + javaFilePath);
            System.exit(1);
        }
    }

    /**
     * Inserts annotations from ajava files into Java files in place.
     *
     * <p>The first argument is a file or directory containing ajava files. It may be a single ajava
     * file or a directory containing ajava files.
     *
     * <p>The second argument is a Java file or a directory containing Java files to insert
     * annotations into.
     *
     * <p>For each file in the second argument, checks if an ajava file from the first argument
     * matches it. For each such file, inserts all its annotations into the Java file.
     *
     * @param args command line arguments, the first element should be a path to ajava files and the
     *     second should be the directory containing Java files to insert into
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println(
                    "Usage: java InsertAjavaAnnotations <ajava-directory> <java-file-directory");
            System.exit(1);
        }

        AnnotationFileStore annotationFiles = new AnnotationFileStore();
        annotationFiles.addFileOrDirectory(new File(args[0]));
        InsertAjavaAnnotations inserter = new InsertAjavaAnnotations(createElements());
        FileVisitor<Path> visitor =
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        if (!path.getFileName().toString().endsWith(".java")) {
                            return FileVisitResult.CONTINUE;
                        }

                        CompilationUnit root = null;
                        try {
                            root = StaticJavaParser.parse(path);
                        } catch (IOException e) {
                            System.err.println("Failed to read file: " + path);
                            System.exit(1);
                        }

                        Set<String> annotationFilesForRoot = new LinkedHashSet<>();
                        for (TypeDeclaration<?> type : root.getTypes()) {
                            String name = type.getNameAsString();
                            if (root.getPackageDeclaration().isPresent()) {
                                name =
                                        root.getPackageDeclaration().get().getNameAsString()
                                                + "."
                                                + name;
                            }

                            annotationFilesForRoot.addAll(
                                    annotationFiles.getAnnotationFileForType(name));
                        }

                        for (String annotationFile : annotationFilesForRoot) {
                            inserter.insertAnnotations(annotationFile, path.toString());
                        }

                        return FileVisitResult.CONTINUE;
                    }
                };

        try {
            Files.walkFileTree(Paths.get(args[1]), visitor);
        } catch (IOException e) {
            System.out.println("Error while adding annotations to: " + args[1]);
            e.printStackTrace();
        }
    }
}
