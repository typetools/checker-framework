package org.checkerframework.framework.ajava;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.PrettyPrinter;
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
            if (node1Annos.getAnnotations().isEmpty()) {
                return;
            }

            StringBuilder insertionContent = new StringBuilder();
            for (AnnotationExpr annotation : node1Annos.getAnnotations()) {
                insertionContent.append(printer.print(annotation));
                insertionContent.append(" ");
            }

            Position position;
            if (node2 instanceof ClassOrInterfaceType) {
                position = ((ClassOrInterfaceType) node2).getName().getBegin().get();
            } else {
                position = node2.getBegin().get();
            }
            int absolutePositition =
                    cumulativeLineSizes.get(position.line - 1) + (position.column - 1);
            insertions.add(new Insertion(absolutePositition, insertionContent.toString()));
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
        annotationFiles.addFile(new File(args[1]));
        FileVisitor<Path> visitor =
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        if (!path.getFileName().endsWith(".java")) {
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
            Files.walkFileTree(Paths.get(args[2]), visitor);
        } catch (IOException e) {
            System.out.println("Error while adding annotations to: " + args[2]);
        }
    }
}
