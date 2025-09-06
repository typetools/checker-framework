package org.checkerframework.javacutil.javacparse;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import org.checkerframework.javacutil.UserError;

/**
 * This class contains static methods that parse Java code.
 *
 * <p>Internally, this class calls the javac parser from the JDK.
 */
public final class JavacParse {

  /** Do not instantiate. */
  private JavacParse() {
    throw new Error("Do not instantiate.");
  }

  /**
   * Parse a Java file.
   *
   * @param filename the file to parse
   * @return a (parsed) compilation unit, which may include parse errors
   * @throws IOException if there is trouble reading the file
   */
  public static JavacParseResult<CompilationUnitTree> parseFile(String filename)
      throws IOException {
    return parseCompilationUnit(new FileJavaFileObject(filename));
  }

  /**
   * Parse a Java file's contents.
   *
   * @param javaCode the contents of a Java file
   * @return a (parsed) compilation unit, which may include parse errors
   */
  public static JavacParseResult<CompilationUnitTree> parseCompilationUnit(String javaCode) {
    try {
      return parseCompilationUnit(new StringJavaFileObject(javaCode));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  /**
   * Parses the given Java type declaration (class, interface, enum, record, etc.).
   *
   * @param classSource the string representation of a Java type declaration
   * @return the parsed type declaration
   */
  public static JavacParseResult<ClassTree> parseTypeDeclaration(String classSource) {
    JavacParseResult<CompilationUnitTree> parsedCU = parseCompilationUnit(classSource);

    // TODO: test for parse error?

    CompilationUnitTree cu = parsedCU.getTree();

    if (!cu.getImports().isEmpty()) {
      throw new IllegalArgumentException(
          "Type declaration source code has imports: " + classSource);
    }
    // CompilationUnitTree.getModule() is defined in Java 17 and later.
    /*
    if (cu.getModule() != null) {
      throw new IllegalArgumentException(
          "Type declaration source code has a module declaration: " + classSource);
    }
    */
    if (cu.getPackage() != null) {
      throw new IllegalArgumentException(
          "Type declaration source code has a package declaration: " + classSource);
    }

    List<? extends Tree> decls = cu.getTypeDecls();
    for (Tree decl : decls) {
      if (decl instanceof EmptyStatementTree) {
        throw new IllegalArgumentException(
            "Type declaration source code contains a top-level `;`: " + classSource);
      }
    }
    if (decls.size() != 1) {
      throw new IllegalArgumentException(
          String.format(
              "Type declaration source code has %d top-level forms, not 1: %s",
              decls.size(), classSource));
    }

    Tree decl = decls.get(0);
    if (decl instanceof ClassTree) {
      return new JavacParseResult<ClassTree>((ClassTree) decl, parsedCU.getDiagnostics());
    } else {
      throw new IllegalArgumentException(
          "source code should be a type declaration but is "
              + decl.getClass().getSimpleName()
              + ":"
              + classSource);
    }
  }

  /**
   * Parses the given Java method or annotation type element.
   *
   * @param methodSource the string representation of a Java expression
   * @return the parsed expression
   */
  public static JavacParseResult<MethodTree> parseMethod(String methodSource) {
    // TODO
    throw new Error("to implement");
  }

  /**
   * Parses the given Java expression string, such as "foo.bar()" or "1 + 2".
   *
   * @param expressionSource the string representation of a Java expression
   * @return the parsed expression
   */
  public static JavacParseResult<ExpressionTree> parseExpression(String expressionSource) {
    // This version may parse a prefix rather than the entire expression.
    // try {
    //   return parseExpression(new StringJavaFileObject(expressionSource));
    // } catch (IOException e) {
    //   throw new Error("This can't happen", e);
    // }

    String dummySource = "class ParseExpression { Object expression = " + expressionSource + "; }";

    JavacParseResult<CompilationUnitTree> cuParse = parseCompilationUnit(dummySource);

    if (cuParse.hasParseError()) {
      String msg = cuParse.getParseErrorMessages();
      if (msg.isEmpty()) {
        throw new Error("Has parse errors, but empty message: " + cuParse.getDiagnostics());
      }
      throw new IllegalArgumentException("Invalid expression (" + msg + "): " + expressionSource);
    }

    CompilationUnitTree cu = cuParse.getTree();

    ClassTree classDecl = (ClassTree) cu.getTypeDecls().get(0);
    List<? extends Tree> members = classDecl.getMembers();
    if (members.size() != 1) {
      // This was an injection attack, such as "0; int x = 1".
      throw new IllegalArgumentException("Invalid expression: " + expressionSource);
    }

    ExpressionTree expr = ((VariableTree) members.get(0)).getInitializer();
    return new JavacParseResult<>(expr, Collections.emptyList());
  }

  /**
   * Parses the given Java type use.
   *
   * @param typeUseSource the string representation of a Java type use
   * @return the parsed type use
   */
  public static JavacParseResult<ExpressionTree> parseTypeUse(String typeUseSource) {
    try {
      return parseTypeUse(new StringJavaFileObject(typeUseSource));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Low-level routines
  //

  // All the routines below this point are copies of one another.

  // Implementation notes:
  // 1. The documentation of Context says "a single Context is used for each invocation of the
  //    compiler".  Re-using the Context causes an error "duplicate context value" in the compiler.
  //    A Context is just a map.
  // 2. Calling `new JavacFileManager` sets a mapping in `context`.  It is necessary to avoid
  //    "this.fileManager is null" error in com.sun.tools.javac.comp.Modules.<init>.

  /**
   * Parse the contents of a JavaFileObject.
   *
   * @param source a JavaFileObject
   * @return a (parsed) compilation unit, which may include parse errors
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult<CompilationUnitTree> parseCompilationUnit(JavaFileObject source)
      throws IOException {
    Context context = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory;
      try {
        parserFactory = ParserFactory.instance(context);
      } catch (IllegalAccessError e) {
        throw new UserError(
            "Provide `--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED` along with"
                + " any other `--add-exports` in the Checker Framework invocation.");
      }
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      CompilationUnitTree cu = parser.parseCompilationUnit();
      ((JCCompilationUnit) cu).sourcefile = source;
      return new JavacParseResult<>(cu, diagnostics.getDiagnostics());
    }
  }

  /**
   * Parse a Java expression.
   *
   * <p><b>Warning:</b> If the prefix of the string is a Java expression, this may return the result
   * of parsing that prefix, even if the whole string is not an expression. For example, it parses
   * "Hello this is nonsense." without error as an identifier "Hello", but it parses "1 +" into a
   * parse error. Therefore, this routine is not appropriate for most uses.
   *
   * @param source a JavaFileObject
   * @return a (parsed) expression, possibly an ErroneousTree
   * @throws IOException if there is trouble reading the file
   * @deprecated may parse a prefix rather than the whole string
   */
  @Deprecated // not for removal
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult<ExpressionTree> parseExpression(JavaFileObject source)
      throws IOException {
    Context context = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      try {
        parserFactory = ParserFactory.instance(context);
      } catch (IllegalAccessError e) {
        throw new UserError(
            "Provide `--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED` along with"
                + " any other `--add-exports` in the Checker Framework invocation.");
      }
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      ExpressionTree eTree = parser.parseExpression();
      return new JavacParseResult<ExpressionTree>(eTree, diagnostics.getDiagnostics());
    }
  }

  /**
   * Parse a type use.
   *
   * @param source a JavaFileObject
   * @return a (parsed) type use, possibly an ErroneousTree
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult<ExpressionTree> parseTypeUse(JavaFileObject source)
      throws IOException {
    Context context = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      try {
        parserFactory = ParserFactory.instance(context);
      } catch (IllegalAccessError e) {
        throw new UserError(
            "Provide `--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED` along with"
                + " any other `--add-exports` in the Checker Framework invocation.");
      }
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      ExpressionTree eTree = parser.parseType();
      return new JavacParseResult<ExpressionTree>(eTree, diagnostics.getDiagnostics());
    }
  }
}
