package org.checkerframework.framework.ajava;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.utils.Pair;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class InsertAjavaAnnotations {
    private static class Insertion {
        public int position;
        public String contents;

        public Insertion(int position, String contents) {
            this.position = position;
            this.contents = contents;
        }
    }

    private static class BuildInsertionsVisitor extends DoubleJavaParserVisitor {
        public List<Insertion> insertions;
        private PrettyPrinter printer;
        private List<Integer> cumulativeLineSizes;

        public BuildInsertionsVisitor(String destFileContents) {
            insertions = new ArrayList<>();
            printer = new PrettyPrinter();
            String[] lines = destFileContents.split(System.lineSeparator());
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

            NodeWithAnnotations<?> node1Annos = (NodeWithAnnotations<?>) node1;
            Position position;
            if (node2 instanceof ClassOrInterfaceType) {
                // In a multi-part name like my.package.MyClass, annotations go directly in front of
                // MyClass instead of the full name.
                position = ((ClassOrInterfaceType) node2).getName().getBegin().get();
            } else {
                position = node2.getBegin().get();
            }

            addAnnotations(position, node1Annos.getAnnotations(), 0, false);
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

            // Transfer import statements.
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

        private int getAbsolutePosition(Position position) {
            return cumulativeLineSizes.get(position.line - 1) + (position.column - 1);
        }
    }

    public static String insertAnnotations(InputStream annotationFile, String fileContents) {
        CompilationUnit annotationCu = StaticJavaParser.parse(annotationFile);
        CompilationUnit fileCu = StaticJavaParser.parse(fileContents);
        BuildInsertionsVisitor insertionVisitor = new BuildInsertionsVisitor(fileContents);
        annotationCu.accept(insertionVisitor, fileCu);
        List<Insertion> insertions = insertionVisitor.insertions;
        insertions.sort(
                (insertion1, insertion2) ->
                        -Integer.compare(insertion1.position, insertion2.position));
        StringBuilder result = new StringBuilder(fileContents);
        for (Insertion insertion : insertions) {
            result.insert(insertion.position, insertion.contents);
        }

        return result.toString();
    }

    public static void insertAnnotations(String annotationFilePath, String javaFilePath) {
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

    public static void main(String[] args) {
        AnnotationFileStore annotationFiles = new AnnotationFileStore();
        annotationFiles.addFile(new File(args[0]));
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
                            insertAnnotations(annotationFile, path.toString());
                        }

                        return FileVisitResult.CONTINUE;
                    }
                };

        try {
            Files.walkFileTree(Paths.get(args[1]), visitor);
        } catch (IOException e) {
            System.out.println("Error while adding annotations to: " + args[1]);
        }
    }
}
