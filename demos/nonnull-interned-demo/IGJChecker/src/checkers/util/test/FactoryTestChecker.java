package checkers.util.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;

import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;

/**
 * A specialized checker for testing purposes.  It verifies that the annotated
 * types of some expression trees are as expected.
 * 
 * The expected types are to be specified in the same test java source file,
 * in a comment either in the same line of the expression tree or the line
 * preceeding it (the comment need to by itself then).
 * The comment should start with '///'.
 * 
 * The comments could be of two forms:
 * <ul>
 *  <li>{@code /// <expected type>}:
 *      to specify the type of the expression in the expression statement</li>
 *  <li>{@code /// <subtree> -:- <expected type>}:
 *      to specify the type of the given subexpression within the line.</li>
 * </ul>
 * 
 * For simplification, the specified types are allowed to be the simple types (e.g. 
 * <i>List<String</i>), instead of the fully qualified names (e.g. 
 * <i>java.util.List<java.lang.String></i>).
 * 
 * Example:
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
 *  The fully qualified name of the custom <i>AnnotatedTypeFactory</i> need to 
 *  be specified through an -Afactory argument (e.g. 
 *  -Afactory=checkers.nonnull.NonNullAnnotatedTypeFactory).  The factory needs
 *  to have a constractor in the form {@code <init>(ProcessingEnvironment, CompilationUnitTree)}
 *  for this checker to construct the factory properly.
 */
/*
 * The code here is one of the most ugliest I have ever written.  I should revise
 * it in the future.  - Mahmood
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions( { "factory" } )
public class FactoryTestChecker extends SourceChecker {
    Class<?> factoryClass;
    Constructor<?> factoryConstructor;
    
    @Override
    public synchronized void init(ProcessingEnvironment p) {
        super.init(p);
        
        // Find factory constructor
        String factoryClassName = env.getOptions().get("factory");
        try {
            if (factoryClassName != null) {
            factoryClass = Class.forName(factoryClassName);
            factoryConstructor = 
                factoryClass.getConstructor(ProcessingEnvironment.class, 
                        CompilationUnitTree.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load " + 
                    factoryClassName + " class.");
        }
    }

    @Override
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        AnnotatedTypeFactory factory;
        try {
            factory = (AnnotatedTypeFactory) factoryConstructor.newInstance(env, root);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't invoke " + 
                    factoryClass.getCanonicalName() + " constructor.");
        }
        return factory;
    }
    
    @Override
    protected Properties getMessages() {
        // We don't have any properties
        Properties prop = new Properties();
        prop.setProperty("type.unexpected", 
                "unexpected type for the given tree\n" + 
                "Tree       : %s\n" +
                "Found      : %s\n" +
                "Expected   : %s\n");
        return prop;
    }

    @Override
    protected SourceVisitor<Void, Void> getSourceVisitor(CompilationUnitTree root) {
        return new ToStringVisitor(this, root);
    }
    
    /**
     * Builds the expected type for the trees from the source file of the 
     * tree compilation unit.
     */
    // This method is extremely ugly
    private Map<TreeSpec, String> buildExpected(CompilationUnitTree tree) {
        Map<TreeSpec, String> expected = new HashMap<TreeSpec, String>();
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
                    if (treeString.endsWith(";"))
                        treeString = treeString.substring(0, treeString.length() - 1);
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber() + 1);
                    expected.put(treeSpec, canonizeTypeString(prevsubtreeMatcher.group(2)));
                } else if (prevfulltreeMatcher.matches()) {
                    String treeString = reader.readLine().trim();
                    if (treeString.endsWith(";"))
                        treeString = treeString.substring(0, treeString.length() - 1);
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber());
                    expected.put(treeSpec, canonizeTypeString(prevfulltreeMatcher.group(1)));
                } else if (subtreeMatcher.matches()) {
                    String treeString = subtreeMatcher.group(2).trim();
                    if (treeString.endsWith(";"))
                        treeString = treeString.substring(0, treeString.length() - 1);
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber());
                    expected.put(treeSpec, canonizeTypeString(subtreeMatcher.group(3)));
                } else if (fulltreeMatcher.matches()) {
                    String treeString = fulltreeMatcher.group(1).trim();
                    if (treeString.endsWith(";"))
                        treeString = treeString.substring(0, treeString.length() - 1);
                    TreeSpec treeSpec = new TreeSpec(treeString.trim(), reader.getLineNumber());
                    expected.put(treeSpec, canonizeTypeString(fulltreeMatcher.group(2)));
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return expected;
    }

    /**
     * A method to canonize the tree representation.
     */
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
     * A method to canonize type string representation.  It removes any unecessary
     * white spaces and finds the type simple name instead of the fully qualified name.
     * 
     * @param str   the type string representation
     * @return  a canonical representation of the type
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
     * A data structure that encapsulate a string and the line number
     * that string appears in the buffer
     */
    private static class TreeSpec {
        public final String treeString;
        public final long lineNumber;
        public TreeSpec(String treeString, long lineNumber) {
            this.treeString = canonizeTreeString(treeString);
            this.lineNumber = lineNumber;
        }
        public int hashCode() {
            return (int) (31 + 3 * treeString.hashCode() + 7 * lineNumber);
        }
        public boolean equals(Object o) {
            if (o instanceof TreeSpec) {
                TreeSpec other = (TreeSpec) o;
                return treeString.equals(other.treeString) 
                    && lineNumber == other.lineNumber;
            } else return false;
        }
        public String toString() {
            return lineNumber + ":" + treeString;
        }
    }

    /**
     * A specialized visitor that compares the actual and expected types
     * for the specified trees and report an error if they differ
     */
    private class ToStringVisitor extends SourceVisitor<Void, Void> {
        Map<TreeSpec, String> expected;
        
        public ToStringVisitor(SourceChecker checker, CompilationUnitTree root) {
            super(checker, root);
            this.expected = buildExpected(root);
        }

        
        @Override
        public Void scan(Tree tree, Void p) {
            if (tree instanceof ExpressionTree) {
                ExpressionTree expTree = (ExpressionTree) tree;
                TreeSpec treeSpec =
                    new TreeSpec(expTree.toString().trim(), 
                            root.getLineMap().getLineNumber(((JCTree)expTree).pos));
                if (expected.containsKey(treeSpec)) {
                String actualType = canonizeTypeString(factory.getAnnotatedType(expTree).toString());
                String expectedType = expected.get(treeSpec);
                if (!actualType.equals(expectedType))
                        FactoryTestChecker.this.report(Result.failure("type.unexpected",
                                tree.toString(), actualType, expectedType), tree);

            }
            }
            return super.scan(tree, p);
        }

    }

}
