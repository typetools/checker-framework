package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import org.checkerframework.framework.util.JavaParserUtil;
import org.checkerframework.javacutil.BugInCF;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores a collection of annotation files. Given a type name, can return a list of paths to stored
 * annotation files corresponding to that type name.
 */
public class AnnotationFileStore {
    /**
     * Mapping from a fully qualified class name to the paths to annotation files that contain that
     * type.
     */
    private Map<String, List<String>> annotationFiles;

    /** Constructs an {@code AnnotationFileStore}. */
    public AnnotationFileStore() {
        annotationFiles = new HashMap<>();
    }

    /**
     * If {@code location} is a file, stores it in this as an annotation file. If {@code location}
     * is a directory, stores all annotation files contained in it.
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
                CompilationUnit root = JavaParserUtil.parseCompilationUnit(location);
                for (TypeDeclaration<?> type : root.getTypes()) {
                    String name = JavaParserUtils.getFullyQualifiedName(type, root);

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
