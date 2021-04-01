package org.checkerframework.framework.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.StubUnit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class JavaParserUtil {

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
    JavaParser javaParser = new JavaParser(new ParserConfiguration());
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
    JavaParser javaParser = new JavaParser(new ParserConfiguration());
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
    JavaParser javaParser = new JavaParser(new ParserConfiguration());
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
    JavaParser javaParser = new JavaParser(new ParserConfiguration());
    ParseResult<StubUnit> parseResult = javaParser.parseStubUnit(inputStream);
    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
      return parseResult.getResult().get();
    } else {
      throw new ParseProblemException(parseResult.getProblems());
    }
  }
}
