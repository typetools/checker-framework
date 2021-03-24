package org.checkerframework.framework.ajava;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.javacutil.BugInCF;

/**
 * Stores a collection of annotation files. Given a type name, can return a list of paths to stored
 * annotation files corresponding to that type name.
 */
public class AnnotationFileStore {
    /**
     * Mapping from fully qualified class names to the collection os paths to annotation files that
     * contain that type.
     */
    private Map<String, List<String>> annotationFiles;

    /** Constructs an {@code AnnotationFileStore}. */
    public AnnotationFileStore() {
        annotationFiles = new HashMap<>();
    }

    /**
     * If {@code location} is a file, stores it as an annotations file, and if {@code location} is a
     * directory, stores all annotations files contained in it recursively.
     *
     * @param location an annotation file or a directory containing annotation files
     */
    public void addFileOrDirectory(File location) {
        if (location.isDirectory()) {
            for (File child : location.listFiles()) {
                addFileOrDirectory(child);
            }

            return;
        }

        if (location.isFile() && location.getName().endsWith(".ajava")) {
            try {
                CompilationUnit root = StaticJavaParser.parse(location);
                for (TypeDeclaration<?> type : root.getTypes()) {
                    String name = type.getNameAsString();
                    if (root.getPackageDeclaration().isPresent()) {
                        name = root.getPackageDeclaration().get().getNameAsString() + "." + name;
                    }

                    if (!annotationFiles.containsKey(name)) {
                        annotationFiles.put(name, new ArrayList<>());
                    }

                    annotationFiles.get(name).add(location.getPath());
                }
            } catch (FileNotFoundException e) {
                throw new BugInCF("Unable to open annotation file: " + location.getPath(), e);
            }
        }
    }

    /**
     * Given a fully qualified type name, returns a List of paths to annotation files containing
     * annotations for the type.
     *
     * @param typeName fully qualified name of a type
     * @return a list of paths to annotation files with annotations for {@code typeName}
     */
    public List<String> getAnnotationFileForType(String typeName) {
        if (!annotationFiles.containsKey(typeName)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(annotationFiles.get(typeName));
    }
}
