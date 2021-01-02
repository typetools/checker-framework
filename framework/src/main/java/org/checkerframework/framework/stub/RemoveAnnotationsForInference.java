package org.checkerframework.framework.stub;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.utils.CollectionStrategy;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.checkerframework.javacutil.BugInCF;

/**
 * Process Java source files to remove annotations that ought to be inferred.
 *
 * <p>Removes annotations from all files in the given directories. Modifies the files in place.
 *
 * <p>Does not remove trusted annotations: those that the checker trusts rather than verifies.
 *
 * <p>Does not remove annotations at locations where inference does no work:
 *
 * <ul>
 *   <li>within the scope of a relevant @SuppressWarnings
 *   <li>within the scope of @IgnoreInWholeProgramInference or an annotation meta-annotated with
 *       that, such as @Option
 * </ul>
 */
public class RemoveAnnotationsForInference {

    /**
     * Processes each provided command-line argument; see {@link RemoveAnnotationsForInference class
     * documentation} for details.
     *
     * @param args command-line arguments: directories to process
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: provide one or more directory names to process");
            System.exit(1);
        }
        for (String arg : args) {
            process(arg);
        }
    }

    /** Maps from simple names to fully-qualified names of annotations. */
    static Multimap<String, String> simpleToFullyQualified = ArrayListMultimap.create();

    static {
        try {
            ClassPath cp = ClassPath.from(RemoveAnnotationsForInference.class.getClassLoader());
            for (ClassPath.ClassInfo ci : cp.getTopLevelClasses()) {
                simpleToFullyQualified.put(ci.getSimpleName(), ci.getName());
            }
        } catch (IOException e) {
            throw new BugInCF(e);
        }
    }

    /**
     * Process each file in the given directory; see the {@link RemoveAnnotationsForInference class
     * documentation} for details.
     *
     * @param dir directory to process
     */
    private static void process(String dir) {

        Path root = JavaStubifier.dirnameToPath(dir);

        MinimizerCallback mc = new MinimizerCallback();
        CollectionStrategy strategy = new ParserCollectionStrategy();
        // Required to include directories that contain a module-info.java, which don't parse by
        // default.
        strategy.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_11);
        ProjectRoot projectRoot = strategy.collect(root);

        projectRoot
                .getSourceRoots()
                .forEach(
                        sourceRoot -> {
                            try {
                                sourceRoot.parse("", mc);
                            } catch (IOException e) {
                                throw new BugInCF(e);
                            }
                        });
    }

    /**
     * Callback to process each Java file; see the {@link RemoveAnnotationsForInference class
     * documentation} for details.
     */
    private static class MinimizerCallback implements SourceRoot.Callback {
        /** The visitor instance. */
        private final MinimizerVisitor mv;

        /** Create a MinimizerCallback instance. */
        public MinimizerCallback() {
            this.mv = new MinimizerVisitor();
        }

        @Override
        public Result process(
                Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
            Optional<CompilationUnit> opt = result.getResult();
            if (opt.isPresent()) {
                CompilationUnit cu = opt.get();
                mv.visit(cu, null);
            }
            return Result.SAVE;
        }
    }

    /**
     * Visitor to process one compilation unit; see the {@link RemoveAnnotationsForInference class
     * documentation} for details.
     */
    private static class MinimizerVisitor extends ModifierVisitor<Void> {

        /**
         * Returns null if the argument should be removed from source code. Returns the argument if
         * it should be retained in source code.
         *
         * @param v an AST node
         * @return the argument to retain it, or null to remove it
         */
        Visitable processAnnotation(Visitable v) {
            if (v == null) {
                return null;
            }
            if (!(v instanceof AnnotationExpr)) {
                throw new BugInCF("What type? %s %s", v.getClass(), v);
            }

            AnnotationExpr n = (AnnotationExpr) v;
            String name = n.getNameAsString();

            // Retain annotations defined in the JDK.
            if (isJdkAnnotation(name)) {
                return n;
            }
            // Retain trusted annotations.
            if (isTrustedAnnotation(name)) {
                return n;
            }
            // Retain annotations for which warnings are suppressed.
            if (isSuppressed(n)) {
                return n;
            }

            // The default behavior is to remove the annotation.
            return null;
        }

        // There are three JavaParser AST nodes that represent annotations

        @Override
        public Visitable visit(final MarkerAnnotationExpr n, final Void arg) {
            Visitable result = super.visit(n, arg);
            return processAnnotation(result);
        }

        @Override
        public Visitable visit(final NormalAnnotationExpr n, final Void arg) {
            Visitable result = super.visit(n, arg);
            return processAnnotation(result);
        }

        @Override
        public Visitable visit(final SingleMemberAnnotationExpr n, final Void arg) {
            Visitable result = super.visit(n, arg);
            return processAnnotation(result);
        }
    }

    /**
     * Returns true if the given annotation is defined in the JDK.
     *
     * @param name the annotation's name (simple or fully-qualified)
     * @return true if the given annotation is defined in the JDK
     */
    static boolean isJdkAnnotation(String name) {
        return name.equals("Serial")
                || name.equals("java.io.Serial")
                || name.equals("Deprecated")
                || name.equals("java.lang.Deprecated")
                || name.equals("FunctionalInterface")
                || name.equals("java.lang.FunctionalInterface")
                || name.equals("Override")
                || name.equals("java.lang.Override")
                || name.equals("SafeVarargs")
                || name.equals("java.lang.SafeVarargs")
                || name.equals("Documented")
                || name.equals("java.lang.annotation.Documented")
                || name.equals("Inherited")
                || name.equals("java.lang.annotation.Inherited")
                || name.equals("Native")
                || name.equals("java.lang.annotation.Native")
                || name.equals("Repeatable")
                || name.equals("java.lang.annotation.Repeatable")
                || name.equals("Retention")
                || name.equals("java.lang.annotation.Retention")
                || name.equals("SuppressWarnings")
                || name.equals("java.lang.SuppressWarnings")
                || name.equals("Target")
                || name.equals("java.lang.annotation.Target");
    }

    /**
     * Returns true if the given annotation is trusted, not checked/verified.
     *
     * @param name the annotation's name (simple or fully-qualified)
     * @return true if the given annotation is trusted, not verified
     */
    static boolean isTrustedAnnotation(String name) {

        // This list was determined by grepping for "trusted" in `qual` directories.
        return name.equals("Untainted")
                || name.equals("org.checkerframework.checker.tainting.qual.Untainted")
                || name.equals("InternedDistinct")
                || name.equals("org.checkerframework.checker.interning.qual.InternedDistinct")
                || name.equals("ReturnsReceiver")
                || name.equals("org.checkerframework.checker.builder.qual.ReturnsReceiver")
                || name.equals("TerminatesExecution")
                || name.equals("org.checkerframework.dataflow.qual.TerminatesExecution")
                || name.equals("Covariant")
                || name.equals("org.checkerframework.framework.qual.Covariant")
                || name.equals("NonLeaked")
                || name.equals("org.checkerframework.common.aliasing.qual.NonLeaked")
                || name.equals("LeakedToResult")
                || name.equals("org.checkerframework.common.aliasing.qual.LeakedToResult");
    }

    /**
     * Returns true if warnings about the given annotation are suppressed.
     *
     * <p>Its heuristic is to look for a {@code @SuppressWarnings} annotation on a containing
     * program element, whose string is one of the elements of the annotation's fully-qualified
     * name.
     *
     * @param arg an annotation
     * @return true if warnings about the given annotation are suppressed
     */
    private static boolean isSuppressed(AnnotationExpr arg) {
        String name = arg.getNameAsString();

        // If it's a simple name for which we know a fully-qualified name, recursively try all
        // fully-qualified names that it could expand to.
        Collection<String> names;
        if (simpleToFullyQualified.containsKey(name)) {
            names = simpleToFullyQualified.get(name);
        } else {
            names = Collections.singletonList(name);
        }

        Iterator<Node> itor = new Node.ParentsVisitor(arg);
        while (itor.hasNext()) {
            Node n = itor.next();
            if (n instanceof NodeWithAnnotations) {
                for (AnnotationExpr ae : ((NodeWithAnnotations<?>) n).getAnnotations()) {
                    if (suppresses(ae, names)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns true if {@code suppressor} suppresses warnings regarding {@code suppressees}.
     *
     * @param suppressor an annotation that might be {@code @SuppressWarnings} or like it
     * @param suppressees an annotation for which warnings might be suppressed. This is actually a
     *     list: if the annotation was written unqualified, it contains all the fully-qualified
     *     names that the unqualified annotation might stand for.
     * @return true if {@code suppressor} suppresses warnings regarding {@code suppressees}
     */
    static boolean suppresses(AnnotationExpr suppressor, Collection<String> suppressees) {
        List<String> suppressWarningsStrings = suppressWarningsStrings(suppressor);
        if (suppressWarningsStrings == null) {
            return false;
        }
        suppressWarningsStrings.replaceAll(RemoveAnnotationsForInference::checkerName);
        // "allcheckers" suppresses all warnings.
        if (suppressWarningsStrings.contains("allcheckers")) {
            return true;
        }

        // Try every element of suppressee's fully-qualified name.
        for (String suppressee : suppressees) {
            for (String fqPart : suppressee.split("\\.")) {
                if (suppressWarningsStrings.contains(fqPart)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Given a @SuppressWarnings annotation, returns its strings. Given an annotation that
     * suppresses warnings, returns strings for what it suppresses. Otherwise, returns null.
     *
     * @param n an annotation
     * @return the (effective) arguments to {@code @SuppressWarnings}, or null
     */
    private static List<String> suppressWarningsStrings(AnnotationExpr n) {
        String name = n.getNameAsString();

        if (name.equals("SuppressWarnings") || name.equals("java.lang.SuppressWarnings")) {
            if (n instanceof MarkerAnnotationExpr) {
                return Collections.emptyList();
            }
            if (n instanceof NormalAnnotationExpr) {
                NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) n).getPairs();
                assert pairs.size() == 1;
                MemberValuePair pair = pairs.get(0);
                assert pair.getName().asString().equals("value");
                return annotationElementStrings(pair.getValue());
            } else if (n instanceof SingleMemberAnnotationExpr) {
                return annotationElementStrings(((SingleMemberAnnotationExpr) n).getMemberValue());
            } else {
                throw new BugInCF("Unexpected AnnotationExpr of type %s: %s", n.getClass(), n);
            }
        }

        if (name.equals("IgnoreInWholeProgramInference")
                || name.equals("org.checkerframework.framework.qual.IgnoreInWholeProgramInference")
                || name.equals("Inject")
                || name.equals("javax.inject.Inject")
                || name.equals("Singleton")
                || name.equals("javax.inject.Singleton")
                || name.equals("Option")
                || name.equals("org.plumelib.options.Option")) {
            List<String> result = new ArrayList<>(1);
            result.add("allcheckers");
            return result;
        }

        return null;
    }

    /**
     * Given an annotation argument for an element of type String[], return a list of strings.
     *
     * @param e an annotation argument
     * @return the strings expressed by {@code e}
     */
    private static List<String> annotationElementStrings(Expression e) {
        if (e instanceof StringLiteralExpr) {
            List<String> result = new ArrayList<>(1);
            result.add(((StringLiteralExpr) e).asString());
            return result;
        } else if (e instanceof ArrayInitializerExpr) {
            NodeList<Expression> values = ((ArrayInitializerExpr) e).getValues();
            List<String> result = new ArrayList<>(values.size());
            for (Expression v : values) {
                if (v instanceof StringLiteralExpr) {
                    result.add(((StringLiteralExpr) v).asString());
                } else {
                    throw new BugInCF(
                            "Unexpected annotation element of type %s: %s", v.getClass(), v);
                }
            }
            return result;
        } else {
            throw new BugInCF("Unexpected %s: %s", e.getClass(), e);
        }
    }

    /**
     * Returns the "checker name" part of a SuppressWarnings string: the part before the colon, or
     * the whole thing if it contains no colon.
     *
     * @param s a SuppressWarnings string: the argument to {@code @SuppressWarnings}
     * @return the part of s before the colon, or the whole thing if it contains no colon
     */
    private static String checkerName(String s) {
        int colonPos = s.indexOf(":");
        if (colonPos == -1) {
            return s;
        } else {
            return s.substring(colonPos + 1);
        }
    }
}
