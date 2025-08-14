package org.checkerframework.framework.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
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

  /** Creates a JavacParseUtil. */
  public JavacParseUtil() {}

  /**
   * Parses the given Java expression string and returns it as a {@link ExpressionTree} using the
   * {@code javac} compiler API.
   *
   * @param expressionSource the string representation of a Java expression (e.g., "foo.bar()", "1 +
   *     2")
   * @return the parsed {@link ExpressionTree}
   * @throws RuntimeException if parsing fails or the expression cannot be found in the AST
   */
  public static ExpressionTree parseExpression(String expressionSource) {
    boolean falze = false;
    if (falze && expressionSource.contains("[]")) {
      System.err.printf("JavacParseUtil.parseExpression(%s)%n", expressionSource);
      new Error("backtrace for " + expressionSource).printStackTrace();
    }

    // This method works by embedding the expression in a dummy class and variable declaration, then
    // parsing the resulting source to extract the expression tree.
    //
    // For example, the input {@code "1 + 2"} is transformed into:
    //   class Dummy { Object expression = 1 + 2; }
    //
    // The initializer of the {@code expression} field is then extracted and returned.

    // Embed the expression inside a dummy class and variable declaration.
    String sanitized = getSanitizedExpressionString(expressionSource);
    String dummySource = "class Dummy { Object expression = " + sanitized + "; }";

    // Obtain the system Java compiler.
    JavaCompiler compiler = JavacTool.create();

    // Create an in-memory Java file from the dummy source code.
    JavaFileObject fileObject =
        new SimpleJavaFileObject(URI.create("string:///Dummy.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return dummySource;
          }
        };

    // Prepare the file manager and task
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
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
      CompilationUnitTree cu = task.parse().iterator().next();

      // Get the first member (the dummy field) from the ClassTree and cast to VariableTree
      ClassTree classTree = (ClassTree) cu.getTypeDecls().get(0);
      VariableTree varTree = (VariableTree) classTree.getMembers().get(0);

      ExpressionTree expr = varTree.getInitializer();
      if (expr == null) {
        throw new RuntimeException("Expression not found in AST.");
      }

      return expr;

    } catch (IOException | IndexOutOfBoundsException | ClassCastException e) {
      throw new RuntimeException("Expression parsing failed", e);
    }
  }

  /**
   * Sanitizes an expression.
   *
   * @param expressionSource the original expression
   * @return the sanitized expression
   */
  public static String getSanitizedExpressionString(String expressionSource) {
    return expressionSource.replaceAll("#num(\\d+)", "\\$num$1");
  }
}
