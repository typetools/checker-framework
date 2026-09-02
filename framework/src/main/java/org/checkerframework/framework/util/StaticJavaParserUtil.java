package org.checkerframework.framework.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.expr.Expression;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Utility methods for working with JavaParser. It is a replacement for {@code
 * com.github.javaparser.StaticJavaParser} that does not leak memory. Also see {@link
 * JavaParserUtil}.
 */
public final class StaticJavaParserUtil {

  /** Do not instantiate. */
  private StaticJavaParserUtil() {
    throw new Error("Do not instantiate.");
  }

  /**
   * The Language Level to use when parsing if a specific level isn't applied. This should be the
   * highest version of Java that the Checker Framework can process.
   */
  public static final LanguageLevel DEFAULT_LANGUAGE_LEVEL = LanguageLevel.JAVA_25;

  //
  // Replacements for StaticJavaParser
  //

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
    parserConfiguration.setPreprocessUnicodeEscapes(true);
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
    configuration.setPreprocessUnicodeEscapes(true);
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
    parserConfiguration.setPreprocessUnicodeEscapes(true);
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
    // The ParserConfiguration accumulates data each time parse is called, so create a new one
    // each time.  There's no method to set the ParserConfiguration used by a JavaParser, so a
    // JavaParser has to be created each time.
    ParserConfiguration configuration = new ParserConfiguration();
    configuration.setLanguageLevel(DEFAULT_LANGUAGE_LEVEL);
    // Store the tokens so that errors have line and column numbers.
    // configuration.setStoreTokens(false);
    configuration.setLexicalPreservationEnabled(false);
    configuration.setAttributeComments(false);
    configuration.setDetectOriginalLineSeparator(false);
    configuration.setPreprocessUnicodeEscapes(true);
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
    // The ParserConfiguration accumulates data each time parse is called, so create a new one
    // each time.  There's no method to set the ParserConfiguration used by a JavaParser, so a
    // JavaParser has to be created each time.
    ParserConfiguration configuration = new ParserConfiguration();
    configuration.setLanguageLevel(languageLevel);
    configuration.setStoreTokens(false);
    configuration.setLexicalPreservationEnabled(false);
    configuration.setAttributeComments(false);
    configuration.setDetectOriginalLineSeparator(false);
    configuration.setPreprocessUnicodeEscapes(true);
    JavaParser javaParser = new JavaParser(configuration);
    ParseResult<Expression> parseResult = javaParser.parseExpression(expression);
    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
      return parseResult.getResult().get();
    } else {
      throw new ParseProblemException(parseResult.getProblems());
    }
  }
}
