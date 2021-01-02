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
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.utils.CollectionStrategy;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.ClassPath;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;

/**
 * Process Java source files to remove annotations that ought to be inferred.
 *
 * <p>Removes annotations from all files in the given directories. Modifies the files in place.
 *
 * <p>Only removes Checker Framework annotations.
 *
 * <p>Does not remove trusted annotations: those that the checker trusts rather than verifies.
 *
 * <p>Does not remove annotations at locations where inference does not work:
 *
 * <ul>
 *   <li>within the scope of a relevant @SuppressWarnings
 *   <li>within the scope of @IgnoreInWholeProgramInference or an annotation meta-annotated with
 *       that, such as @Option
 * </ul>
 */
public class RemoveAnnotationsForInference {

    public static boolean debug = true;

    /**
     * Processes each provided command-line argument; see class documentation for details.
     *
     * @param args command-line arguments: directories to process
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: provide one or more directory names to process");
            System.exit(1);
        }
        RemoveAnnotationsForInference rafi = new RemoveAnnotationsForInference();
        for (String arg : args) {
            rafi.process(arg);
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
     * Process each file in the given directory; see class documentation for details.
     *
     * @param dir directory to process
     */
    private void process(String dir) {

        if (debug) {
            System.out.printf("process(%s)%n", dir);
        }

        File f = new File(dir);
        if (!f.isDirectory()) {
            System.err.printf("Not a directory: %s (%s).%n", dir, f);
            System.exit(1);
        }
        if (!f.exists()) {
            System.err.printf("Directory %s (%s) does not exist.%n", dir, f);
            System.exit(1);
        }
        String dirName = f.getAbsolutePath();
        if (dirName.endsWith("/.")) {
            dirName = dirName.substring(0, dirName.length() - 2);
        }
        Path root = Paths.get(dirName);
        System.out.printf("root = %s%n", root);
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
                                System.err.println("IOException: " + e);
                            }
                        });
    }

    /** Callback to process each Java file; see class documentation for details. */
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
            Result res = Result.SAVE;
            if (debug) {
                System.out.printf("Removing annotations from %s%n", absolutePath);
            }
            Optional<CompilationUnit> opt = result.getResult();
            if (opt.isPresent()) {
                CompilationUnit cu = opt.get();
                mv.visit(cu, null);
            }
            return res;
        }
    }

    /**
     * Given a @SuppressWarnings annotation, returns its strings.
     *
     * @param n a @SuppressWarnings annotation
     * @return the strings that are the element of the given annotation
     */
    private static List<String> suppressWarningsStrings(AnnotationExpr n) {
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

    /**
     * Given an expression written as an annotation argument for an element of type String[], return
     * a list of strings.
     *
     * @param e an annotation argument
     * @return the strings expressed by {@code e}
     */
    private static List<String> annotationElementStrings(Expression e) {
        if (e instanceof StringLiteralExpr) {
            return Collections.singletonList(((StringLiteralExpr) e).asString());
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

    /** Visitor to process one compilation unit; see class documentation for details. */
    private static class MinimizerVisitor extends ModifierVisitor<Void> {

        /** Records what @SuppressWarnings enclose the current parse position. */
        SuppressionStack suppressionStack = new SuppressionStack();

        /**
         * Returns null if the argument should be removed from source code. Returns the argument if
         * it should be retained in source code.
         *
         * @param v an AST node
         * @return the argument to retain it, or null to remove it
         */
        Visitable processAnnotation(Visitable v) {
            if (debug) {
                System.out.printf("processAnnotation(%s)%n", v);
            }
            if (v == null) {
                if (debug) {
                    System.out.printf("processAnnotation(null) => null%n");
                }
                return null;
            }
            if (!(v instanceof AnnotationExpr)) {
                throw new BugInCF("What type? %s %s", v.getClass(), v);
            }
            AnnotationExpr n = (AnnotationExpr) v;

            String name = n.getNameAsString();

            if (name.equals("SuppressWarnings") || name.equals("java.lang.SuppressWarnings")) {
                suppressionStack.addAll(suppressWarningsStrings(n));
                if (debug) {
                    System.out.printf(
                            "processAnnotation(%s) => self (is Suppresswarningsstrings)%n", v);
                    System.out.println(suppressionStack.toStringDebug());
                }

                return n;
            }

            // Don't remove most annotations defined in the JDK
            if (name.equals("Serial")
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
                    || name.equals("Target")
                    || name.equals("java.lang.annotation.Target")) {
                suppressionStack.addAll(suppressWarningsStrings(n));
                if (debug) {
                    System.out.printf("processAnnotation(%s) => self (is in JDK)%n", v);
                }

                return n;
            }

            if (name.equals("IgnoreInWholeProgramInference")
                    || name.equals(
                            "org.checkerframework.framework.qual.IgnoreInWholeProgramInference")
                    || name.equals("Inject")
                    || name.equals("javax.inject.Inject")
                    || name.equals("Singleton")
                    || name.equals("javax.inject.Singleton")
                    || name.equals("Option")
                    || name.equals("org.plumelib.options.Option")) {
                // Potential problem:  Other annotations might have appeared before this annotation
                // and might have already been processed and incorrectly removed.
                suppressionStack.add("allcheckers");
                if (debug) {
                    System.out.printf("processAnnotation(%s) => self (reflection)%n", v);
                }
                return n;
            }

            if (isSuppressed(name)) {
                if (debug) {
                    System.out.printf("processAnnotation(%s) => null (isSuppressed)%n", v);
                }
                return n;
            }

            // The default behavior is to remove the annotation.
            if (debug) {
                System.out.printf("processAnnotation(%s) => null (fallthrough)%n", v);
            }
            return null;
        }

        /**
         * Returns true if warnings about the given annotation are suppressed.
         *
         * @param name an annotation name
         * @return true if warnings about the given annotation are suppressed
         */
        boolean isSuppressed(String name) {
            if (debug) {
                System.out.printf(
                        "isSuppressed(%s), fq=%s%n", name, simpleToFullyQualified.get(name));
                if (name.equals("InternedDistinct")) {
                    System.out.println(suppressionStack.toStringDebug());
                }
            }

            // If it's a simple name for which we know a fully-qualified name, recursively try all
            // fully-qualified names that it could expand to.
            if (simpleToFullyQualified.containsKey(name)) {
                for (String fq : simpleToFullyQualified.get(name)) {
                    if (suppressionStack.isSuppressed(fq)) {
                        return true;
                    }
                }
                return false;
            } else {
                return suppressionStack.isSuppressed(name);
            }
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

        // TODO: What about variable declarations?

        // The subclasses of NodeWithAnnotations are the following.  Their implementations contain
        // suppressionStack.pushFrame() and suppressionStack.popFrame() calls.
        //
        // AnnotationDeclaration
        // AnnotationMemberDeclaration
        // ArrayCreationLevel -- no nesting
        // ArrayType -- no nesting
        // BodyDeclaration -- not a leaf
        // CallableDeclaration -- not a leaf
        // ClassOrInterfaceDeclaration
        // ClassOrInterfaceType
        // ConstructorDeclaration
        // EnumConstantDeclaration
        // EnumDeclaration
        // FieldDeclaration
        // InitializerDeclaration
        // IntersectionType
        // MethodDeclaration
        // ModuleDeclaration
        // PackageDeclaration
        // Parameter
        // PrimitiveType
        // ReceiverParameter
        // TypeDeclaration -- not a leaf
        // TypeParameter
        // UnionType
        // VariableDeclarationExpr
        // VoidType
        // WildcardType

        @Override
        public Visitable visit(final AnnotationDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final AnnotationMemberDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ArrayCreationLevel n, final Void arg) {
            suppressionStack.pushFrame(n.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ArrayType n, final Void arg) {
            suppressionStack.pushFrame(n.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ClassOrInterfaceDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ClassOrInterfaceType n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ConstructorDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final EnumConstantDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final EnumDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final FieldDeclaration n, final Void arg) {
            StringJoiner sj = new StringJoiner(", ");
            for (VariableDeclarator var : n.getVariables()) {
                sj.add(var.getNameAsString());
            }
            suppressionStack.pushFrame(sj.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final InitializerDeclaration n, final Void arg) {
            suppressionStack.pushFrame("InitializerDeclaration");
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final MethodDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ModuleDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final PackageDeclaration n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final Parameter n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final PrimitiveType n, final Void arg) {
            suppressionStack.pushFrame(n.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ReceiverParameter n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final TypeParameter n, final Void arg) {
            suppressionStack.pushFrame(n.getNameAsString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final UnionType n, final Void arg) {
            suppressionStack.pushFrame(n.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final VariableDeclarationExpr n, final Void arg) {
            StringJoiner sj = new StringJoiner(", ");
            for (VariableDeclarator var : n.getVariables()) {
                sj.add(var.getNameAsString());
            }
            suppressionStack.pushFrame(sj.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final VoidType n, final Void arg) {
            suppressionStack.pushFrame(n.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final WildcardType n, final Void arg) {
            suppressionStack.pushFrame(n.toString());
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }
    }

    /**
     * Maintain a stack of suppressions. Each frame is a string (for debugging) and a list of
     * strings. Each String in the list is an argument to "@SuppressWarnings".
     */
    private static class SuppressionStack extends ArrayDeque<Pair<String, List<String>>> {
        static final long serialVersionUID = 20201227;

        /**
         * Adds a frame to this.
         *
         * @param s information about the frame (for debugging).
         */
        void pushFrame(String s) {
            addFirst(Pair.of(s, new ArrayList<>()));
        }

        /** Removes a frame from this. */
        void popFrame() {
            removeFirst();
        }

        /**
         * Adds the given suppression string to this. Ignores any part after a colon.
         *
         * @param s an argument to {@code @SuppressWarnings}
         */
        void add(String s) {
            int colonPos = s.indexOf(":");
            if (colonPos != -1) {
                s = s.substring(colonPos + 1);
            }
            getFirst().second.add(s);
        }

        /**
         * Adds all the given suppression strings to this.
         *
         * @param strings arguments to {@code @SuppressWarnings}
         * @see #add
         */
        void addAll(Iterable<String> strings) {
            for (String s : strings) {
                add(s);
            }
        }

        /**
         * Returns true if the string appears exactly in this data structure.
         *
         * @param s a string
         * @return true if the string appears exactly in this data structure
         */
        boolean contains(String s) {
            for (Pair<String, List<String>> p : this) {
                if (p.second.contains(s)) {
                    if (debug) {
                        System.out.printf("contains(%s) => true%n", s);
                        System.out.println(this.toStringDebug());
                    }
                    return true;
                }
            }
            if (debug) {
                System.out.printf("contains(%s) => false%n", s);
            }
            return false;
        }

        /**
         * Returns true if warnings about the given annotation name are suppressed. Its heuristic is
         * to look for {@code @SuppressWarnings} annotation whose string is one of the elements of
         * the annotation's fully-qualified name.
         *
         * @param annoName a simple or fully-qualified annotation name
         * @return true if warnings abuot the annotation are suppressed
         */
        boolean isSuppressed(String annoName) {
            if (debug) {
                System.out.printf("SuppressionStack.isSuppressed(%s)%n", annoName);
            }

            // "allcheckers" suppresses all warnings.
            if (contains("allcheckers")) {
                return true;
            }

            // Try every element of its fully-qualified name.
            for (String fqPart : annoName.split("\\.")) {
                if (contains(fqPart)) {
                    if (debug) {
                        System.out.printf("SuppressionStack.isSuppressed(%s) => true%n", annoName);
                    }

                    return true;
                }
            }
            if (debug) {
                System.out.printf("SuppressionStack.isSuppressed(%s) => false%n", annoName);
            }
            return false;
        }

        /**
         * Returns a verbose, multiline description of this.
         *
         * @return a verbose, multiline description of this
         */
        String toStringDebug() {
            StringJoiner sj = new StringJoiner(System.lineSeparator());
            for (Pair<String, List<String>> p : this) {
                sj.add(p.first + ": " + p.second.toString());
            }
            return sj.toString();
        }
    }
}
