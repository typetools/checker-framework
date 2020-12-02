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

public class AnnotationFileStore {
    /**
     * Mapping from fully qualified class names to the collection os paths to annotation files that
     * contain that type.
     */
    private Map<String, List<String>> annotationFiles;

    public AnnotationFileStore() {
        annotationFiles = new HashMap<>();
    }

    public void addFile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                addFile(child);
            }

            return;
        }

        if (file.isFile() && file.getName().endsWith(".ajava")) {
            try {
                CompilationUnit root = StaticJavaParser.parse(file);
                for (TypeDeclaration<?> type : root.getTypes()) {
                    String name = type.getNameAsString();
                    if (root.getPackageDeclaration().isPresent()) {
                        name = root.getPackageDeclaration().get().getNameAsString() + "." + name;
                    }

                    if (!annotationFiles.containsKey(name)) {
                        annotationFiles.put(name, new ArrayList<>());
                    }

                    annotationFiles.get(name).add(file.getPath());
                }
            } catch (FileNotFoundException e) {
                throw new BugInCF("Unable to open annotation file: " + file.getPath(), e);
            }
        }
    }

    public List<String> getAnnotationFileForType(String typeName) {
        if (!annotationFiles.containsKey(typeName)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(annotationFiles.get(typeName));
    }
}
