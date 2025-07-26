package org.checkerframework.framework.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTool;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * Utility class that provides methods for parsing Java expressions using the {@code javac} compiler
 * API.
 *
 * <p>This utility uses {@link JavacTask} and the standard {@code com.sun.source.tree} API to parse
 * expressions.
 */
public class JavacParseUtil {

  /**
   * Parses the given Java expression string and returns it as a {@link ExpressionTree} using the
   * {@code javac} compiler API.
   *
   * <p>The method works by embedding the expression in a dummy class and variable declaration, then
   * parsing the resulting source to extract the expression tree.
   *
   * <p>For example, the input {@code "1 + 2"} is transformed into:
   *
   * <pre>{@code
   * class Dummy { Object expression = 1 + 2; }
   * }</pre>
   *
   * The initializer of the {@code expression} field is then extracted and returned.
   *
   * @param expressionSource the string representation of a Java expression (e.g., "foo.bar()", "1 +
   *     2")
   * @return the parsed {@link ExpressionTree}
   * @throws RuntimeException if parsing fails or the expression cannot be found in the AST
   */
  public static ExpressionTree parseExpression(String expressionSource) {
    // Embed the expression inside a dummy class and variable declaration
    String dummySource = "class Dummy { Object expression = " + expressionSource + "; }";

    // Obtain the system Java compiler
    JavaCompiler compiler = JavacTool.create();

    // Create an in-memory Java file from the dummy source code
    JavaFileObject fileObject =
        new SimpleJavaFileObject(URI.create("string:///Dummy.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return dummySource;
          }
        };

    try {
      // Prepare the file manager and task
      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      JavacTask task =
          (JavacTask)
              compiler.getTask(
                  null,
                  fileManager,
                  null,
                  Collections.emptyList(),
                  null,
                  Collections.singletonList(fileObject));

      // Parse the source and extract the CompilationUnit
      Iterable<? extends CompilationUnitTree> trees = task.parse();
      CompilationUnitTree cu = trees.iterator().next();

      // Use a TreeScanner to extract the expression from the variable initializer
      ExpressionExtractor extractor = new ExpressionExtractor();
      for (Tree typeDecl : cu.getTypeDecls()) {
        typeDecl.accept(extractor, null);
      }

      if (extractor.result == null) {
        throw new RuntimeException("Expression not found in AST.");
      }

      return extractor.result;

    } catch (IOException e) {
      throw new RuntimeException("Expression Parsing failed", e);
    }
  }

  /**
   * A TreeScanner that locates the dummy variable declaration and captures its initializer (i.e.,
   * the target expression being parsed).
   */
  private static class ExpressionExtractor extends TreeScanner<Void, Void> {
    ExpressionTree result = null;

    @Override
    public Void visitVariable(VariableTree node, Void p) {
      // Look for the "expression" variable introduced in dummy source
      if (node.getName().contentEquals("expression")) {
        result = node.getInitializer();
      }
      return super.visitVariable(node, p);
    }
  }
}
