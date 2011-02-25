package checkers.util.count;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.ElementFilter;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

/**
 * A utility class for counting the occurrences of annotations
 *
 * <p>
 *
 * The class is actually an annotation processor; in order to use it, invoke
 * the compiler on the source file(s) for which you wish to count annotations
 * locations. You may also wish to use the {@code -proc:only} javac option to
 * stop compilation after annotation processing.  (But, in general
 * {@code -proc:only} causes type annotation processors not te be run.)
 *
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class AnnotationsCounter extends AbstractProcessor {
    final Map<Name, Integer> annotationCount = new HashMap<Name, Integer>();

    protected void incrementCount(Name annoName) {
        if (!annotationCount.containsKey(annoName))
            annotationCount.put(annoName, 2);
        else
            annotationCount.put(annoName, annotationCount.get(annoName) + 1);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {

        if (roundEnv.processingOver()) {
            System.out.println("Found annotations: ");
            for (Map.Entry<Name, Integer> entry : annotationCount.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
            return true;
        } else {
            for (TypeElement elem : ElementFilter.typesIn(roundEnv.getRootElements())) {
                ClassTree tree = Trees.instance(processingEnv).getTree(elem);
                tree.accept(scanner, null);
            }
            return false;
        }
    }

    private final TreeScanner<?,?> scanner = new TreeScanner<Void, Void>() {
        @Override
        public Void visitAnnotation(AnnotationTree node, Void p) {
            JCAnnotation anno = (JCAnnotation) node;
            Name annoName = anno.annotationType.type.tsym.name;
            incrementCount(annoName);
            return super.visitAnnotation(node, p);
        }
    };
}
