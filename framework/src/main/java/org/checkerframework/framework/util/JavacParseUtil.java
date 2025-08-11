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
import java.util.regex.Pattern;
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

  // Pattern to reject clearly invalid expressions before parsing
  private static final Pattern EXPRESSION_GATE =
      Pattern.compile(
          "^(?!.*;)\\s*"
              + "(?!\\[?\\s*error\\s+for\\s+expression:)"
              + "(?:(?!(?:final\\s+)?(?:byte|short|int|long|float|double|boolean|char|var)\\b.*=).)*"
              + "(?:(?!(?:if|switch|for|while|do|try|catch|finally|return|throw|break|continue|class|interface|enum)\\b).)*"
              + ".+\\S.*$",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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
    String sanitized = expressionSource.replaceAll("#num(\\d+)", "\\$num$1").trim();

    // Quick pre-check to skip obvious non-expressions
    if (!EXPRESSION_GATE.matcher(sanitized).matches()) {
      throw new RuntimeException("Not a valid Java expression: " + expressionSource);
    }

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
}
