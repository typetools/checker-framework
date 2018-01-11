package org.checkerframework.common.util.count;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import org.checkerframework.javacutil.AbstractTypeProcessor;

/**
 * An annotation processor for counting the occurrences of annotations. To invoke it, use
 *
 * <pre>
 * javac -proc:only -processor org.checkerframework.common.util.count.AnnotationsCounter <em>MyFile.java ...</em>
 * </pre>
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationsCounter extends AbstractTypeProcessor {
    final Map<Name, Integer> annotationCount = new HashMap<Name, Integer>();

    protected void incrementCount(Name annoName) {
        if (!annotationCount.containsKey(annoName)) {
            annotationCount.put(annoName, 1);
        } else {
            annotationCount.put(annoName, annotationCount.get(annoName) + 1);
        }
    }

    @Override
    public void typeProcess(TypeElement element, TreePath tree) {
        tree.getLeaf().accept(scanner, null);
    }

    @Override
    public void typeProcessingOver() {
        if (annotationCount.isEmpty()) {
            System.out.println("No annotations found.");
        } else {
            System.out.println("Found annotations: ");
            for (Map.Entry<Name, Integer> entry : annotationCount.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
        }
    }

    private final TreeScanner<?, ?> scanner =
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitAnnotation(AnnotationTree node, Void p) {
                    JCAnnotation anno = (JCAnnotation) node;
                    Name annoName = anno.annotationType.type.tsym.getQualifiedName();
                    incrementCount(annoName);
                    return super.visitAnnotation(node, p);
                }
            };
}
