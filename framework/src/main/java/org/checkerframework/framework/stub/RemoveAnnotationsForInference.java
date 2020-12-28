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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.checkerframework.javacutil.BugInCF;

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
        Path root = Paths.get(dir);
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
            // System.out.printf("Minimizing %s%n", absolutePath);
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

        SuppressionStack suppressionStack = new SuppressionStack();

        // Returns null if the argument should be removed from source code.
        // Returns the argument if it should be retained in source code.
        Visitable processAnnotation(Visitable v) {
            if (v == null) {
                return null;
            }
            if (!(v instanceof AnnotationExpr)) {
                throw new BugInCF("What type? %s %s", v.getClass(), v);
            }
            AnnotationExpr n = (AnnotationExpr) v;

            String name = n.getNameAsString();

            if (name.equals("SuppressWarnings") || name.equals("java.lang. SuppressWarnings")) {
                suppressionStack.addAll(suppressWarningsStrings(n));
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
                suppressionStack.add("allcheckers");
                return n;
            }

            if (suppressionStack.isSuppressed(name)) {
                return null;
            }

            return n;
        }

        boolean isSuppressed(AnnotationExpr n) {
            String name = n.getName().toString();

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
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final AnnotationMemberDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ArrayCreationLevel n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ArrayType n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ClassOrInterfaceDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ClassOrInterfaceType n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ConstructorDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final EnumConstantDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final EnumDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final FieldDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final InitializerDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final MethodDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ModuleDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final PackageDeclaration n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final Parameter n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final PrimitiveType n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final ReceiverParameter n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final TypeParameter n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final UnionType n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final VariableDeclarationExpr n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final VoidType n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }

        @Override
        public Visitable visit(final WildcardType n, final Void arg) {
            suppressionStack.pushFrame();
            Visitable result = super.visit(n, arg);
            suppressionStack.popFrame();
            return result;
        }
    }

    /**
     * Maintain a stack of suppressions. Each frame is a list of strings. Each String is an argument
     * to "@SuppressWarnings".
     */
    private static class SuppressionStack extends ArrayDeque<List<String>> {
        static final long serialVersionUID = 20201227;

        /** Adds a frame to this. */
        void pushFrame() {
            addFirst(new ArrayList<>());
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
            getFirst().add(s);
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
            for (List<String> l : this) {
                if (l.contains(s)) {
                    return true;
                }
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
            // "allcheckers" suppresses all warnings.
            if (contains("allcheckers")) {
                return true;
            }

            // Try every element of its fully-qualified name.
            for (String fqPart : annoName.split("\\.")) {
                if (contains(fqPart)) {
                    return true;
                }
            }
            return false;
        }
    }
}
