package org.checkerframework.framework.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.javacutil.BugInCF;

/**
 * Utility methods for working with JavaParser. It is a replacement for StaticJavaParser that does
 * not leak memory, and it provides some other methods.
 */
public class JavaParserUtil {

  /**
   * The Language Level to use when parsing if a specific level isn't applied. This should be the
   * highest version of Java that the Checker Framework can process. Currently, Java 11.
   */
  public static LanguageLevel DEFAULT_LANGUAGE_LEVEL = LanguageLevel.JAVA_11;

  ///
  /// Replacements for StaticJavaParser
  ///

  /**
   * Parses the Java code contained in the {@code InputStream} and returns a {@code CompilationUnit}
   * that represents it.
   *
   * <p>This is like {@code StaticJavaParser.parse}, but it does not lead to memory leaks because it
   * creates a new instance of JavaParser each time it is invoked. Re-using {@code StaticJavaParser}
   * causes memory problems because it retains too much memory.
   *
   * @param inputStream the Java source code
   * @return CompilationUnit representing the Java source code
   * @throws ParseProblemException if the source code has parser errors
   */
  public static CompilationUnit parseCompilationUnit(InputStream inputStream) {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setLanguageLevel(DEFAULT_LANGUAGE_LEVEL);
    JavaParser javaParser = new JavaParser(parserConfiguration);
    ParseResult<CompilationUnit> parseResult = javaParser.parse(inputStream);
    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
      return parseResult.getResult().get();
    } else {
      throw new ParseProblemException(parseResult.getProblems());
    }
  }

  /**
   * Parses the Java code contained in the {@code File} and returns a {@code CompilationUnit} that
   * represents it.
   *
   * <p>This is like {@code StaticJavaParser.parse}, but it does not lead to memory leaks because it
   * creates a new instance of JavaParser each time it is invoked. Re-using {@code StaticJavaParser}
   * causes memory problems because it retains too much memory.
   *
   * @param file the Java source code
   * @return CompilationUnit representing the Java source code
   * @throws ParseProblemException if the source code has parser errors
   * @throws FileNotFoundException if the file was not found
   */
  public static CompilationUnit parseCompilationUnit(File file) throws FileNotFoundException {
    ParserConfiguration configuration = new ParserConfiguration();
    configuration.setLanguageLevel(DEFAULT_LANGUAGE_LEVEL);
    JavaParser javaParser = new JavaParser(configuration);
    ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
      return parseResult.getResult().get();
    } else {
      throw new ParseProblemException(parseResult.getProblems());
    }
  }

  /**
   * Parses the Java code contained in the {@code String} and returns a {@code CompilationUnit} that
   * represents it.
   *
   * <p>This is like {@code StaticJavaParser.parse}, but it does not lead to memory leaks because it
   * creates a new instance of JavaParser each time it is invoked. Re-using {@code StaticJavaParser}
   * causes memory problems because it retains too much memory.
   *
   * @param javaSource the Java source code
   * @return CompilationUnit representing the Java source code
   * @throws ParseProblemException if the source code has parser errors
   */
  public static CompilationUnit parseCompilationUnit(String javaSource) {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setLanguageLevel(DEFAULT_LANGUAGE_LEVEL);
    JavaParser javaParser = new JavaParser(parserConfiguration);
    ParseResult<CompilationUnit> parseResult = javaParser.parse(javaSource);
    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
      return parseResult.getResult().get();
    } else {
      throw new ParseProblemException(parseResult.getProblems());
    }
  }

  /**
   * Parses the stub file contained in the {@code InputStream} and returns a {@code StubUnit} that
   * represents it.
   *
   * <p>This is like {@code StaticJavaParser.parse}, but it does not lead to memory leaks because it
   * creates a new instance of JavaParser each time it is invoked. Re-using {@code StaticJavaParser}
   * causes memory problems because it retains too much memory.
   *
   * @param inputStream the stub file
   * @return StubUnit representing the stub file
   * @throws ParseProblemException if the source code has parser errors
   */
  public static StubUnit parseStubUnit(InputStream inputStream) {
    // The ParserConfiguration accumulates data each time parse is called, so create a new one each
    // time.  There's no method to set the ParserConfiguration used by a JavaParser, so a JavaParser
    // has to be created each time.
    ParserConfiguration configuration = new ParserConfiguration();
    configuration.setLanguageLevel(DEFAULT_LANGUAGE_LEVEL);
    // Store the tokens so that errors have line and column numbers.
    // configuration.setStoreTokens(false);
    configuration.setLexicalPreservationEnabled(false);
    configuration.setAttributeComments(false);
    configuration.setDetectOriginalLineSeparator(false);
    JavaParser javaParser = new JavaParser(configuration);
    ParseResult<StubUnit> parseResult = javaParser.parseStubUnit(inputStream);
    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
      return parseResult.getResult().get();
    } else {
      throw new ParseProblemException(parseResult.getProblems());
    }
  }

  /**
   * Parses the {@code expression} and returns an {@code Expression} that represents it.
   *
   * <p>This is like {@code StaticJavaParser.parseExpression}, but it does not lead to memory leaks
   * because it creates a new instance of JavaParser each time it is invoked. Re-using {@code
   * StaticJavaParser} causes memory problems because it retains too much memory.
   *
   * @param expression the expression string
   * @return the parsed expression
   * @throws ParseProblemException if the expression has parser errors
   */
  public static Expression parseExpression(String expression) {
    return parseExpression(expression, DEFAULT_LANGUAGE_LEVEL);
  }

  /**
   * Parses the {@code expression} and returns an {@code Expression} that represents it.
   *
   * <p>This is like {@code StaticJavaParser.parseExpression}, but it does not lead to memory leaks
   * because it creates a new instance of JavaParser each time it is invoked. Re-using {@code
   * StaticJavaParser} causes memory problems because it retains too much memory.
   *
   * @param expression the expression string
   * @param languageLevel the language level to use when parsing the Java source
   * @return the parsed expression
   * @throws ParseProblemException if the expression has parser errors
   */
  public static Expression parseExpression(String expression, LanguageLevel languageLevel) {
    // The ParserConfiguration accumulates data each time parse is called, so create a new one each
    // time.  There's no method to set the ParserConfiguration used by a JavaParser, so a JavaParser
    // has to be created each time.
    ParserConfiguration configuration = new ParserConfiguration();
    configuration.setLanguageLevel(languageLevel);
    configuration.setStoreTokens(false);
    configuration.setLexicalPreservationEnabled(false);
    configuration.setAttributeComments(false);
    configuration.setDetectOriginalLineSeparator(false);
    JavaParser javaParser = new JavaParser(configuration);
    ParseResult<Expression> parseResult = javaParser.parseExpression(expression);
    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
      return parseResult.getResult().get();
    } else {
      throw new ParseProblemException(parseResult.getProblems());
    }
  }

  ///
  /// Other methods
  ///

  /**
   * Given the compilation unit node for a source file, returns the top level type definition with
   * the given name.
   *
   * @param root compilation unit to search
   * @param name name of a top level type declaration in {@code root}
   * @return a top level type declaration in {@code root} named {@code name}
   */
  public static TypeDeclaration<?> getTypeDeclarationByName(CompilationUnit root, String name) {
    Optional<ClassOrInterfaceDeclaration> classDecl = root.getClassByName(name);
    if (classDecl.isPresent()) {
      return classDecl.get();
    }

    Optional<ClassOrInterfaceDeclaration> interfaceDecl = root.getInterfaceByName(name);
    if (interfaceDecl.isPresent()) {
      return interfaceDecl.get();
    }

    Optional<EnumDeclaration> enumDecl = root.getEnumByName(name);
    if (enumDecl.isPresent()) {
      return enumDecl.get();
    }

    Optional<CompilationUnit.Storage> storage = root.getStorage();
    if (storage.isPresent()) {
      throw new BugInCF("Type " + name + " not found in " + storage.get().getPath());
    } else {
      throw new BugInCF("Type " + name + " not found in " + root);
    }
  }

  /**
   * Returns the fully qualified name of a type appearing in a given compilation unit.
   *
   * @param type a type declaration
   * @param compilationUnit the compilation unit containing {@code type}
   * @return the fully qualified name of {@code type} if {@code compilationUnit} contains a package
   *     declaration, or just the name of {@code type} otherwise
   */
  public static String getFullyQualifiedName(
      TypeDeclaration<?> type, CompilationUnit compilationUnit) {
    if (compilationUnit.getPackageDeclaration().isPresent()) {
      return compilationUnit.getPackageDeclaration().get().getNameAsString()
          + "."
          + type.getNameAsString();
    } else {
      return type.getNameAsString();
    }
  }

  /**
   * Side-effects {@code node} by removing all annotations from anywhere inside its subtree.
   *
   * @param node a JavaParser Node
   */
  public static void clearAnnotations(Node node) {
    node.accept(new ClearAnnotationsVisitor(), null);
  }

  /** A visitor that clears all annotations from a JavaParser AST. */
  private static class ClearAnnotationsVisitor extends VoidVisitorWithDefaultAction {
    @Override
    public void defaultAction(Node node) {
      for (Node child : new ArrayList<>(node.getChildNodes())) {
        if (child instanceof AnnotationExpr) {
          node.remove(child);
        }
      }
    }
  }

  /**
   * Side-effects node by combining any added String literals in node's subtree into their
   * concatenation. For example, the expression {@code "a" + "b"} becomes {@code "ab"}. This occurs
   * even if, when reading from left to right, the two string literals are not added directly. For
   * example, the expression {@code 1 + "a" + "b"} parses as {@code (1 + "a") + "b"}}, but it is
   * transformed into {@code 1 + "ab"}.
   *
   * <p>This is the same transformation performed by javac automatically. Javac seems to ignore
   * string literals surrounded in parentheses, so this method does as well.
   *
   * @param node a JavaParser Node
   */
  public static void concatenateAddedStringLiterals(Node node) {
    node.accept(new StringLiteralConcatenateVisitor(), null);
  }

  /** Visitor that combines added String literals, see {@link #concatenateAddedStringLiterals}. */
  public static class StringLiteralConcatenateVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(BinaryExpr node, Void p) {
      super.visit(node, p);
      if (node.getOperator() == BinaryExpr.Operator.PLUS && node.getRight().isStringLiteralExpr()) {
        String right = node.getRight().asStringLiteralExpr().asString();
        if (node.getLeft().isStringLiteralExpr()) {
          String left = node.getLeft().asStringLiteralExpr().asString();
          node.replace(new StringLiteralExpr(left + right));
        } else if (node.getLeft().isBinaryExpr()) {
          BinaryExpr leftExpr = node.getLeft().asBinaryExpr();
          if (leftExpr.getOperator() == BinaryExpr.Operator.PLUS
              && leftExpr.getRight().isStringLiteralExpr()) {
            String left = leftExpr.getRight().asStringLiteralExpr().asString();
            node.replace(
                new BinaryExpr(
                    leftExpr.getLeft(),
                    new StringLiteralExpr(left + right),
                    BinaryExpr.Operator.PLUS));
          }
        }
      }
    }
  }

  /**
   * Returns the {@link com.github.javaparser.ParserConfiguration.LanguageLevel} corresponding to
   * the current source version.
   *
   * @param env processing environment used to get source version
   * @return the current source version
   */
  public static ParserConfiguration.LanguageLevel getCurrentSourceVersion(
      ProcessingEnvironment env) {
    // Use String comparison so we can compile on older JDKs which
    // don't have all the latest SourceVersion constants:
    switch (env.getSourceVersion().name()) {
      case "RELEASE_8":
        return ParserConfiguration.LanguageLevel.JAVA_8;
      case "RELEASE_9":
        return ParserConfiguration.LanguageLevel.JAVA_9;
      case "RELEASE_10":
        return ParserConfiguration.LanguageLevel.JAVA_10;
      case "RELEASE_11":
        return ParserConfiguration.LanguageLevel.JAVA_11;
      case "RELEASE_12":
        return ParserConfiguration.LanguageLevel.JAVA_12;
      case "RELEASE_13":
        return ParserConfiguration.LanguageLevel.JAVA_13;
      case "RELEASE_14":
        return ParserConfiguration.LanguageLevel.JAVA_14;
      case "RELEASE_15":
        return ParserConfiguration.LanguageLevel.JAVA_15;
      case "RELEASE_16":
        return ParserConfiguration.LanguageLevel.JAVA_16;
      default:
        return ParserConfiguration.LanguageLevel.JAVA_8;
    }
  }
}
