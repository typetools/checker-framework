package testlib.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A specialized checker for testing purposes. It compares an expression's annotated type to an
 * expected type.
 *
 * <p>The expected type is written in a stylized comment (starting with '///') in the same Java
 * source file. The comment appears either on the same line as the expression, or else by itself on
 * the line preceding the expression.
 *
 * <p>The comments are of two forms:
 *
 * <ul>
 *   <li>{@code /// <expected type>}: to specify the type of the expression in the expression
 *       statement
 *   <li>{@code /// <subtree> -:- <expected type>}: to specify the type of the given subexpression
 *       within the line.
 * </ul>
 *
 * The specified types are allowed to use simple names (e.g., {@code List<String>}), instead of
 * fully qualified names (e.g., {@code java.util.List<java.lang.String>}).
 *
 * <p>Example:
 *
 * <pre>
 *  void test() {
 *      // Comments in the same line
 *      Collections.<@NonNull String>emptyList();  /// List<@NonNull String>
 *      List<@NonNull String> l = Collections.emptyList(); /// Collections.emptyList() -:- List<@NonNull String>
 *
 *      // Comments in the previous lines
 *      /// List<@NonNull String>
 *      Collections.<@NonNull String>emptyList();
 *
 *      /// Collections.emptyList() -:- List<@NonNull String>
 *      List<@NonNull String> l = Collections.emptyList();
 *  }
 * </pre>
 *
 * The fully qualified name of the custom <i>AnnotatedTypeFactory</i> is specified through an {@code
 * -Afactory} command-line argument (e.g. {@code
 * -Afactory=checkers.nullness.NullnessAnnotatedTypeFactory}). The factory needs to have a
 * constructor of the form {@code <init>(ProcessingEnvironment, CompilationUnitTree)}.
 */
/*
 * The code here is one of the most ugliest I have ever written.  I should revise
 * it in the future.  - Mahmood
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"checker"})
public class FactoryTestChecker extends BaseTypeChecker {
    SourceChecker checker;

    @Override
    public void initChecker() {
        super.initChecker();

        // Find factory constructor
        String checkerClassName = getOption("checker");
        try {
            if (checkerClassName != null) {
                Class<?> checkerClass = Class.forName(checkerClassName);
                Constructor<?> constructor = checkerClass.getConstructor();
                Object o = constructor.newInstance();
                if (o instanceof SourceChecker) {
                    checker = (SourceChecker) o;
                }
            }
        } catch (Exception e) {
            throw new BugInCF("Couldn't load " + checkerClassName + " class.");
        }
    }

    /*
    @Override
    public AnnotatedTypeFactory createTypeFactory() {
        return checker.createTypeFactory();
    }*/

    @Override
    public Properties getMessages() {
        // We don't have any properties
        // '\n' doesn't need to be replaced here
        Properties prop = new Properties();
        prop.setProperty(
                "type.unexpected",
                "unexpected type for the given tree\n"
                        + "Tree       : %s\n"
                        + "Found      : %s\n"
                        + "Expected   : %s\n");
        return prop;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ToStringVisitor(this);
    }

    /** Builds the expected type for the trees from the source file of the tree compilation unit. */
    // This method is extremely ugly
    private Map<TreeSpec, String> buildExpected(CompilationUnitTree tree) {
        Map<TreeSpec, String> expected = new HashMap<>();
        try {
            JavaFileObject o = tree.getSourceFile();
            File sourceFile = new File(o.toUri());
            LineNumberReader reader = new LineNumberReader(new FileReader(sourceFile));
            String line = reader.readLine();
            Pattern prevsubtreePattern = Pattern.compile("\\s*///(.*)-:-(.*)");
            Pattern prevfulltreePattern = Pattern.compile("\\s*///(.*)");
            Pattern subtreePattern = Pattern.compile("(.*)///(.*)-:-(.*)");
            Pattern fulltreePattern = Pattern.compile("(.*)///(.*)");
            while (line != null) {
                Matcher prevsubtreeMatcher = prevsubtreePattern.matcher(line);
                Matcher prevfulltreeMatcher = prevfulltreePattern.matcher(line);
                Matcher subtreeMatcher = subtreePattern.matcher(line);
                Matcher fulltreeMatcher = fulltreePattern.matcher(line);
                if (prevsubtreeMatcher.matches()) {
                    String treeString = prevsubtreeMatcher.group(1).trim();
                    if (treeString.endsWith(";")) {
                        treeString = treeString.substring(0, treeString.length() - 1);
                    }
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber() + 1);
                    expected.put(treeSpec, canonizeTypeString(prevsubtreeMatcher.group(2)));
                } else if (prevfulltreeMatcher.matches()) {
                    String treeString = reader.readLine().trim();
                    if (treeString.endsWith(";")) {
                        treeString = treeString.substring(0, treeString.length() - 1);
                    }
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber());
                    expected.put(treeSpec, canonizeTypeString(prevfulltreeMatcher.group(1)));
                } else if (subtreeMatcher.matches()) {
                    String treeString = subtreeMatcher.group(2).trim();
                    if (treeString.endsWith(";")) {
                        treeString = treeString.substring(0, treeString.length() - 1);
                    }
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber());
                    expected.put(treeSpec, canonizeTypeString(subtreeMatcher.group(3)));
                } else if (fulltreeMatcher.matches()) {
                    String treeString = fulltreeMatcher.group(1).trim();
                    if (treeString.endsWith(";")) {
                        treeString = treeString.substring(0, treeString.length() - 1);
                    }
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber());
                    expected.put(treeSpec, canonizeTypeString(fulltreeMatcher.group(2)));
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return expected;
    }

    /** A method to canonize the tree representation. */
    private static String canonizeTreeString(String str) {
        String canon = str.trim();
        Pattern pattern = Pattern.compile("(@\\S+)\\(\\)");
        Matcher matcher = pattern.matcher(canon);
        while (matcher.find()) {
            canon = matcher.replaceFirst(matcher.group(1));
            matcher.reset(canon);
        }
        return canon.trim();
    }

    /**
     * A method to canonize type string representation. It removes any unnecessary white spaces and
     * finds the type simple name instead of the fully qualified name.
     *
     * @param str the type string representation
     * @return a canonical representation of the type
     */
    private static String canonizeTypeString(String str) {
        String canon = str.trim();
        canon = canon.replaceAll("\\s+", " ");
        // Remove spaces between [ ]
        canon = canon.replaceAll("\\[\\s+", "[");
        canon = canon.replaceAll("\\s+\\]", "]");

        // Remove spaces between < >
        canon = canon.replaceAll("<\\s+", "<");
        canon = canon.replaceAll("\\s+>", ">");

        // Take simply names!
        canon = canon.replaceAll("[^\\<]*\\.(?=\\w)", "");
        return canon;
    }

    /**
     * A data structure that encapsulate a string and the line number that string appears in the
     * buffer
     */
    private static class TreeSpec {
        public final String treeString;
        public final long lineNumber;

        public TreeSpec(String treeString, long lineNumber) {
            this.treeString = canonizeTreeString(treeString);
            this.lineNumber = lineNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(treeString, lineNumber);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TreeSpec) {
                TreeSpec other = (TreeSpec) o;
                return treeString.equals(other.treeString) && lineNumber == other.lineNumber;
            }
            return false;
        }

        @Override
        public String toString() {
            return lineNumber + ":" + treeString;
        }
    }

    /**
     * A specialized visitor that compares the actual and expected types for the specified trees and
     * report an error if they differ
     */
    private class ToStringVisitor extends BaseTypeVisitor<GenericAnnotatedTypeFactory<?, ?, ?, ?>> {
        Map<TreeSpec, String> expected;

        public ToStringVisitor(BaseTypeChecker checker) {
            super(checker);
            this.expected = buildExpected(root);
        }

        @Override
        public Void scan(Tree tree, Void p) {
            if (TreeUtils.isExpressionTree(tree)) {
                ExpressionTree expTree = (ExpressionTree) tree;
                TreeSpec treeSpec =
                        new TreeSpec(
                                expTree.toString().trim(),
                                root.getLineMap().getLineNumber(((JCTree) expTree).pos));
                if (expected.containsKey(treeSpec)) {
                    String actualType =
                            canonizeTypeString(atypeFactory.getAnnotatedType(expTree).toString());
                    String expectedType = expected.get(treeSpec);
                    if (!actualType.equals(expectedType)) {

                        // The key is added above using a setProperty call, which is not supported
                        // by the CompilerMessageChecker
                        @SuppressWarnings("compilermessages")
                        Result res =
                                Result.failure(
                                        "type.unexpected",
                                        tree.toString(),
                                        actualType,
                                        expectedType);
                        FactoryTestChecker.this.report(res, tree);
                    }
                }
            }
            return super.scan(tree, p);
        }
    }
}
