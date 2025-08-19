package org.checkerframework.framework.util.javacparse;

import com.sun.source.tree.Tree;
import java.util.List;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Represents the result of parsing Java code (a file or a subpart thereof).
 *
 * @param <T> the type of the Java code being parsed
 */
public final class JavacParseResult<T extends Tree> {

  /** The parse tree. */
  private final T tree;

  /** The diagnostics. */
  private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

  /**
   * Create a JavacParseResult.
   *
   * @param tree the parse tree
   * @param diagnostics the diagnostics
   */
  public JavacParseResult(T tree, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    this.tree = tree;
    this.diagnostics = diagnostics;
  }

  /**
   * Returns the parse tree.
   *
   * @return the parse tree
   */
  public final T getTree() {
    return tree;
  }

  /**
   * Returns the diagnostics.
   *
   * @return the diagnostics
   */
  public final List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
    return diagnostics;
  }

  /**
   * Returns true if at least one diagnostic is a parse error.
   *
   * @return true if at least one diagnostic is a parse error
   */
  public final boolean hasParseError() {
    return diagnostics.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
  }

  /**
   * Returns all the parse error messages, concatenated. May return an empty string.
   *
   * @return all the parse error messages, concatenated
   */
  public final String getParseErrorMessages() {
    StringJoiner sj = new StringJoiner("; ");
    for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
      if (d.getKind() == Diagnostic.Kind.ERROR) {
        @SuppressWarnings("nullness:argument") // javac is not annotated
        String msg = d.getMessage(null);
        sj.add(msg);
      }
    }
    return sj.toString();
  }
}
