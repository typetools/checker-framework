package checkers.util.count;

import checkers.source.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A utility class for listing the potential locations of annotations.
 *
 * <p>
 *
 * The class is actually an annotation processor; in order to use it, invoke
 * the compiler on the source file(s) for which you wish to count annotations
 * locations, and supply the argument:
 * <pre>-processor checkers.util.count.Locations</pre>
 *
 *
 * <p>
 *
 * Counting the number of lines of the processor's output yields the annotation
 * location count (e.g., by piping the output to {@code wc}). Because the
 * processor outputs a single line of text describing type of each annotation
 * location it encounters, you can obtain the count for specific annotation
 * location types (i.e., possible local variable annotations, or possible
 * method receiver annotations) by filtering the output accordingly (e.g., with
 * {@code grep}).
 *
 * <p>
 *
 * By default, this utility displays annotation locations only. The following
 * two options may be used to adjust the output:
 *
 * <ul>
 *  <li>{@code -Anolocations}: suppresses location output</li>
 *  <li>{@code -Aannotations}: enables annotation output</li>
 * </ul>
 */
@SupportedOptions({"nolocations", "annotations"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class Locations extends SourceChecker {

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new Visitor(this, root);
    }

    static class Visitor extends SourceVisitor<Void, Void> {

        /** Whether annotation locations should be printed. */
        private final boolean locations;

        /** Whether annotation details should be printed. */
        private final boolean annotations;

        public Visitor(Locations l, CompilationUnitTree root) {
            super(l, root);

            // Get annotation processor "-A" options.
            Map<String, String> options = l.getProcessingEnvironment().getOptions();
            locations = !options.containsKey("nolocations");
            annotations = options.containsKey("annotations");
        }

        private static final String LINE_SEPARATOR = System.getProperty("line.separator");

        @Override
        public Void visitAnnotation(AnnotationTree tree, Void p) {
            if (annotations) {

                // An annotation is a body annotation if, while ascending the
                // AST from the annotation to the root, we find a block
                // immediately enclosed by a method.
                //
                // If an annotation is not a body annotation, it's a signature
                // (declaration) annotation.

                boolean isBodyAnnotation = false;
                TreePath path = getCurrentPath();
                Tree prev = null;
                for (Tree t : path) {
                    if (prev != null && prev.getKind() == Tree.Kind.BLOCK
                            && t.getKind() == Tree.Kind.METHOD) {
                        isBodyAnnotation = true;
                        break;
                    }
                    prev = t;
                }

                System.out.printf(":annotation %s %s %s %s%s",
                        tree.getAnnotationType(),
                        tree,
                        root.getSourceFile().getName(),
                        (isBodyAnnotation ? "body" : "sig"),
                        LINE_SEPARATOR);
            }
            return super.visitAnnotation(tree, p);
        }

        @Override
        public Void visitArrayType(ArrayTypeTree tree, Void p) {
            if (locations)
                System.out.println("array type");
            return super.visitArrayType(tree, p);
        }

        @Override
        public Void visitClass(ClassTree tree, Void p) {
            if (locations) {
                System.out.println("class");
                if (tree.getExtendsClause() != null)
                    System.out.println("class extends");
                for (Tree t : tree.getImplementsClause())
                    System.out.println("class implements");
            }
            return super.visitClass(tree, p);
        }

        @Override
        public Void visitMethod(MethodTree tree, Void p) {
            if (locations) {
                System.out.println("method return");
                System.out.println("method receiver");
                for (Tree t : tree.getThrows())
                    System.out.println("method throws");
                for (Tree t : tree.getParameters())
                    System.out.println("method param");
            }
            return super.visitMethod(tree, p);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void p) {
            if (locations)
                System.out.println("variable");
            return super.visitVariable(tree, p);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
            if (locations) {
                for (Tree t : tree.getTypeArguments())
                    System.out.println("method invocation type argument");
            }
            return super.visitMethodInvocation(tree, p);
        }

        @Override
        public Void visitNewClass(NewClassTree tree, Void p) {
            if (locations) {
                System.out.println("new class");
                for (Tree t : tree.getTypeArguments())
                    System.out.println("new class type argument");
            }
            return super.visitNewClass(tree, p);
        }

        @Override
        public Void visitNewArray(NewArrayTree tree, Void p) {
            if (locations) {
                System.out.println("new array");
                for (Tree t : tree.getDimensions())
                    System.out.println("new array dimension");
            }
            return super.visitNewArray(tree, p);
        }

        @Override
        public Void visitTypeCast(TypeCastTree tree, Void p) {
            if (locations)
                System.out.println("typecast");
            return super.visitTypeCast(tree, p);
        }

        @Override
        public Void visitInstanceOf(InstanceOfTree tree, Void p) {
            if (locations)
                System.out.println("instanceof");
            return super.visitInstanceOf(tree, p);
        }

        @Override
        public Void visitParameterizedType(ParameterizedTypeTree tree, Void p) {
            if (locations) {
                for (Tree t : tree.getTypeArguments())
                    System.out.println("parameterized type");
            }
            return super.visitParameterizedType(tree, p);
        }

        @Override
        public Void visitTypeParameter(TypeParameterTree tree, Void p) {
            if (locations) {
                for (Tree t : tree.getBounds())
                    System.out.println("type parameter bound");
            }
            return super.visitTypeParameter(tree, p);
        }

        @Override
        public Void visitWildcard(WildcardTree tree, Void p) {
            if (locations)
                System.out.println("wildcard");
            return super.visitWildcard(tree, p);
        }
    }
}
