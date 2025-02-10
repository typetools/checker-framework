package org.checkerframework.framework.stub;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.visitor.GenericListVisitorAdapter;
import com.github.javaparser.utils.CollectionStrategy;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.PositionUtils;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.ClassPath;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.stubifier.JavaStubifier;
import org.checkerframework.framework.util.JavaParserUtil;
import org.checkerframework.javacutil.BugInCF;
import org.plumelib.util.ArraysPlume;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.StringsPlume;

/**
 * Process Java source files to remove annotations that ought to be inferred.
 *
 * <p>Removes annotations from all files in the given directories. Modifies the files in place.
 *
 * <p>Does not remove trusted annotations: those that the checker trusts rather than verifies.
 *
 * <p>Also does not remove annotations that the user requests to keep in the source code. Provide a
 * list of annotations to keep via the {@code -keepFile} command line argument, which must be the
 * first argument to this program if it is present. The second argument to the program should be the
 * path to the keep file. The keep file itself should be a list of newline-separated annotation
 * names (without {@literal @} symbols). Both the simple and fully-qualified name of each annotation
 * usually should be included in the keep file (simple string-matching between the annotations in
 * the keep file and the annotation names used in the source code whose annotations are being
 * removed is used for annotation comparison). TODO: remove this restriction?
 *
 * <p>Does not remove annotations at locations where inference does no work:
 *
 * <ul>
 *   <li>within the scope of a relevant @SuppressWarnings
 *   <li>within the scope of @IgnoreInWholeProgramInference or an annotation meta-annotated with
 *       that, such as @Option
 * </ul>
 *
 * After removing annotations, javac may issue "warning: [cast] redundant cast to ..." if {@code
 * -Alint:cast} (or {@code -Alint:all} which implies it) is passed to javac. You can suppress the
 * warning by passing {@code -Alint:-cast} to javac.
 */
public class RemoveAnnotationsForInference {

  /**
   * Do not instantiate. This is a standalone program whose entry point is {@link #main(String[])}.
   */
  private RemoveAnnotationsForInference() {
    throw new Error("Do not instantiate RemoveAnnotationsForInference.");
  }

  /**
   * A list of annotations not to remove (i.e., to keep in the source code). Used to prevent
   * project-specific annotations that must remain for the project to build from being removed by
   * this program. (It would be burdensome to add all project-specific annotations to the global
   * list in {@link #isTrustedAnnotation(String)}.)
   */
  private static @MonotonicNonNull Set<String> annotationsToKeep = null;

  /**
   * Processes each provided command-line argument; see {@link RemoveAnnotationsForInference class
   * documentation} for details.
   *
   * @param args command-line arguments: directories to process
   */
  public static void main(String[] args) {
    // TODO: using plume-lib's "Options" project here would be better, but would add a
    // dependency to the whole Checker Framework, which is undesirable. Move this program
    // elsewhere (e.g., to a plume-lib project)?
    if (args[0].contentEquals("-keepFile")) {
      if (args.length < 2) {
        System.err.println(
            "Usage: -keepFile requires an argument immediately after it: the path to the keep"
                + " file.");
        System.exit(2);
      }
      String keepFilePath = args[1];
      try (Stream<String> lines = Files.lines(Paths.get(keepFilePath))) {
        annotationsToKeep = lines.collect(Collectors.toSet());
      } catch (FileNotFoundException e) {
        System.err.println("Error: Keep file " + keepFilePath + " not found.");
        System.exit(3);
      } catch (IOException e) {
        System.err.println("Problem reading keep file " + keepFilePath + ": " + e.getMessage());
        System.exit(4);
      }

      // Check for common mistake of adding "@" before the annotation name.
      for (String annotationToKeep : annotationsToKeep) {
        if (annotationToKeep.startsWith("@")) {
          System.err.println(
              "Error: Keep file includes an @ symbol before this annotation: "
                  + annotationToKeep
                  + ". Annotations should be listed in the keep file without the @ symbol.");
          System.exit(5);
        }
      }

      args = ArraysPlume.subarray(args, 2, args.length - 2);
    }
    if (args.length < 1) {
      System.err.println("Usage: provide one or more directory names to process");
      System.exit(1);
    }
    for (String arg : args) {
      process(arg);
    }
  }

  /**
   * Maps from simple names to fully-qualified names of annotations. (Actually, it includes every
   * class on the classpath.)
   */
  static Multimap<String, String> simpleToFullyQualified = ArrayListMultimap.create();

  static {
    try {
      ClassPath cp = ClassPath.from(RemoveAnnotationsForInference.class.getClassLoader());
      for (ClassPath.ClassInfo ci : cp.getTopLevelClasses()) {
        // There is no way to determine whether `ci` represents an annotation, without
        // loading it.
        // I could filter using a heuristic: only include classes in a package named "qual".
        simpleToFullyQualified.put(ci.getSimpleName(), ci.getName());
      }
    } catch (IOException e) {
      throw new BugInCF(e);
    }
  }

  /**
   * Process each file in the given directory; see the {@link RemoveAnnotationsForInference class
   * documentation} for details.
   *
   * @param dir directory to process
   */
  private static void process(String dir) {

    Path root = JavaStubifier.dirnameToPath(dir);

    RemoveAnnotationsCallback rac = new RemoveAnnotationsCallback();
    CollectionStrategy strategy = new ParserCollectionStrategy();
    // Required to include directories that contain a module-info.java, which don't parse by
    // default.
    strategy.getParserConfiguration().setLanguageLevel(JavaParserUtil.DEFAULT_LANGUAGE_LEVEL);
    ProjectRoot projectRoot = strategy.collect(root);

    for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
      try {
        sourceRoot.parse("", rac);
      } catch (IOException e) {
        throw new BugInCF(e);
      }
    }
  }

  /**
   * Callback to process each Java file; see the {@link RemoveAnnotationsForInference class
   * documentation} for details.
   */
  private static class RemoveAnnotationsCallback implements SourceRoot.Callback {
    /** The visitor instance. */
    private final RemoveAnnotationsVisitor rav = new RemoveAnnotationsVisitor();

    @Override
    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
      Optional<CompilationUnit> opt = result.getResult();
      if (opt.isPresent()) {
        CompilationUnit cu = opt.get();
        List<AnnotationExpr> removals = rav.visit(cu, null);
        removeAnnotations(absolutePath, removals);
      }
      return Result.DONT_SAVE;
    }
  }

  // An earlier implementation used ModifierVisitor.  However, JavaParser's unparser can change
  // the structure of the program. For example, it changes `protected @Nullable Object x;` to
  // `@Nullable protected Object x;` which yields a type.anno.before.modifier error.

  /**
   * Rewrites the file in place, removing the given annotations from it.
   *
   * @param absolutePath the path to the file
   * @param removals the annotations to remove
   */
  static void removeAnnotations(Path absolutePath, List<AnnotationExpr> removals) {
    if (removals.isEmpty()) {
      return;
    }

    List<String> lines;
    try {
      lines = Files.readAllLines(absolutePath);
    } catch (IOException e) {
      System.out.printf("Problem reading %s: %s%n", absolutePath, e.getMessage());
      System.exit(1);
      throw new Error("unreachable");
    }

    PositionUtils.sortByBeginPosition(removals);
    Collections.reverse(removals);

    // This code (correctly) assumes that no element of `removals` is contained within another.
    for (AnnotationExpr removal : removals) {
      Position begin = removal.getBegin().get();
      Position end = removal.getEnd().get();
      int beginLine = begin.line - 1;
      int beginColumn = begin.column - 1;
      int endLine = end.line - 1;
      int endColumn = end.column; // a JavaParser range is inclusive of the character at "end"
      if (beginLine == endLine) {
        String line = lines.get(beginLine);
        String prefix = line.substring(0, beginColumn);
        String suffix = line.substring(endColumn);

        // Remove whitespace to beautify formatting.
        suffix = CharMatcher.whitespace().trimLeadingFrom(suffix);
        if (suffix.startsWith("[")) {
          prefix = CharMatcher.whitespace().trimTrailingFrom(prefix);
        }

        String newLine = prefix + suffix;
        replaceLine(lines, beginLine, newLine);
      } else {
        String newLastLine = lines.get(endLine).substring(endColumn);
        replaceLine(lines, endLine, newLastLine);
        for (int lineno = endLine - 1; lineno > beginLine; lineno--) {
          lines.remove(lineno);
        }
        String newFirstLine = lines.get(beginLine).substring(0, beginColumn);
        replaceLine(lines, beginLine, newFirstLine);
      }
    }

    try (PrintWriter pw =
        new PrintWriter(
            Files.newBufferedWriter(Paths.get(absolutePath.toString()), StandardCharsets.UTF_8))) {
      for (String line : lines) {
        pw.println(line);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("problem writing " + absolutePath.toString(), e);
    }
  }

  /**
   * If {@code newLine} is blank, removes the given line. Otherwise replaces the given line.
   *
   * @param lines the list in which to do replacement or removal
   * @param lineno the index of the line to be removed or replaced
   * @param newLine the new line for index {@code lineno}
   */
  static void replaceLine(List<String> lines, int lineno, String newLine) {
    if (StringsPlume.isBlank(newLine)) {
      lines.remove(lineno);
    } else {
      lines.set(lineno, newLine);
    }
  }

  /**
   * Visits one compilation unit, collecting the annotations that should be removed. See the {@link
   * RemoveAnnotationsForInference class documentation} for more details.
   *
   * <p>The annotations will be removed from the source code by the {@link #removeAnnotations}
   * method.
   */
  private static class RemoveAnnotationsVisitor
      extends GenericListVisitorAdapter<AnnotationExpr, Void> {

    /**
     * Returns annotations that should be removed from source code.
     *
     * @param n an annotation
     * @param superResult the result of calling {@code super.visit} on n; this includes processing
     *     the subcomponents of n
     * @return the argument to remove it, or superResult to retain it
     */
    List<AnnotationExpr> processAnnotation(AnnotationExpr n, List<AnnotationExpr> superResult) {
      if (n == null) {
        // TODO: How is this possible?
        return superResult;
      }

      String name = n.getNameAsString();

      // Retain annotations defined in the JDK.
      if (isJdkAnnotation(name)) {
        return superResult;
      }
      // Retain trusted annotations.
      if (isTrustedAnnotation(name)) {
        return superResult;
      }
      // Retain annotations that the user requested specifically should be kept.
      if (shouldBeKept(name)) {
        return superResult;
      }
      // Retain annotations for which warnings are suppressed.
      if (isSuppressed(n)) {
        return superResult;
      }

      // The default behavior is to remove the annotation.
      // Don't include superResult, which is contained within `n`.
      return Collections.singletonList(n);
    }

    // There are three JavaParser AST nodes that represent annotations

    @Override
    public List<AnnotationExpr> visit(MarkerAnnotationExpr n, Void arg) {
      return processAnnotation(n, super.visit(n, arg));
    }

    @Override
    public List<AnnotationExpr> visit(NormalAnnotationExpr n, Void arg) {
      return processAnnotation(n, super.visit(n, arg));
    }

    @Override
    public List<AnnotationExpr> visit(SingleMemberAnnotationExpr n, Void arg) {
      return processAnnotation(n, super.visit(n, arg));
    }
  }

  /**
   * Returns true if the given annotation is defined in the JDK.
   *
   * @param name the annotation's name (simple or fully-qualified)
   * @return true if the given annotation is defined in the JDK
   */
  static boolean isJdkAnnotation(String name) {
    return name.equals("Serial")
        || name.equals("java.io.Serial")
        || name.equals("Deprecated")
        || name.equals("java.lang.Deprecated")
        || name.equals("FunctionalInterface")
        || name.equals("java.lang.FunctionalInterface")
        || name.equals("Override")
        || name.equals("java.lang.Override")
        || name.equals("SafeVarargs")
        || name.equals("java.lang.SafeVarargs")
        || name.equals("Documented")
        || name.equals("java.lang.annotation.Documented")
        || name.equals("Inherited")
        || name.equals("java.lang.annotation.Inherited")
        || name.equals("Native")
        || name.equals("java.lang.annotation.Native")
        || name.equals("Repeatable")
        || name.equals("java.lang.annotation.Repeatable")
        || name.equals("Retention")
        || name.equals("java.lang.annotation.Retention")
        || name.equals("SuppressWarnings")
        || name.equals("java.lang.SuppressWarnings")
        || name.equals("Target")
        || name.equals("java.lang.annotation.Target");
  }

  /**
   * Returns true if the given annotation is trusted, not checked/verified.
   *
   * @param name the annotation's name (simple or fully-qualified)
   * @return true if the given annotation is trusted, not verified
   */
  static boolean isTrustedAnnotation(String name) {
    // This list was determined by grepping for "trusted" in `qual` directories.
    return name.equals("Untainted")
        || name.equals("org.checkerframework.checker.tainting.qual.Untainted")
        || name.equals("InternedDistinct")
        || name.equals("org.checkerframework.checker.interning.qual.InternedDistinct")
        || name.equals("ReturnsReceiver")
        || name.equals("org.checkerframework.checker.builder.qual.ReturnsReceiver")
        || name.equals("TerminatesExecution")
        || name.equals("org.checkerframework.dataflow.qual.TerminatesExecution")
        || name.equals("Covariant")
        || name.equals("org.checkerframework.framework.qual.Covariant")
        || name.equals("NonLeaked")
        || name.equals("org.checkerframework.common.aliasing.qual.NonLeaked")
        || name.equals("LeakedToResult")
        || name.equals("org.checkerframework.common.aliasing.qual.LeakedToResult");
  }

  /**
   * Returns true iff the annotation is present in the user-supplied file of annotations to keep
   * (via the {@code -keepFile} command-line option).
   *
   * @param name the annotation's name (simple or fully-qualified)
   * @return true if the user requested that this annotation be kept in the source code
   */
  static boolean shouldBeKept(String name) {
    return (annotationsToKeep != null && annotationsToKeep.contains(name));
  }

  // This approach searches upward to find all the active warning suppressions.
  // An alternative, more efficient approach would be to track the current set of warning
  // suppressions, using a stack.
  // There are two problems with the alternative approach (and besides, this approach is fast
  // enough as it is).
  //  1. JavaParser sometimes visits members before the annotation, so there was not a chance to
  //     observe the annotation and place it on the suppression stack.  This should be fixed for
  //     ModifierVisitor (but not for other visitors such as GenericListVisitorAdapter) in
  //     JavaParser release 3.19.0.
  //  2. A user might write an annotation before @SuppressWarnings, as in:
  //       @Interned @SuppressWarnings("interning")
  //     The {@code @Interned} annotation is visited before the {@code @SuppressWarnings}
  //     annotation is.  This could be addressed by searching just the parent's annotations.

  /**
   * Returns true if warnings about the given annotation are suppressed.
   *
   * <p>Its heuristic is to look for a {@code @SuppressWarnings} annotation on a containing program
   * element, whose string is one of the elements of the annotation's fully-qualified name.
   *
   * @param arg an annotation
   * @return true if warnings about the given annotation are suppressed
   */
  private static boolean isSuppressed(AnnotationExpr arg) {
    String name = arg.getNameAsString();

    // If it's a simple name for which we know a fully-qualified name,
    // try all fully-qualified names that it could expand to.
    Collection<String> names;
    if (simpleToFullyQualified.containsKey(name)) {
      names = simpleToFullyQualified.get(name);
    } else {
      names = Collections.singletonList(name);
    }

    Iterator<Node> itor = new Node.ParentsVisitor(arg);
    while (itor.hasNext()) {
      Node n = itor.next();
      if (n instanceof NodeWithAnnotations) {
        for (AnnotationExpr ae : ((NodeWithAnnotations<?>) n).getAnnotations()) {
          if (suppresses(ae, names)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns true if {@code suppressor} suppresses warnings regarding {@code suppressees}.
   *
   * @param suppressor an annotation that might be {@code @SuppressWarnings} or like it
   * @param suppressees an annotation for which warnings might be suppressed. This is actually a
   *     list: if the annotation was written unqualified, it contains all the fully-qualified names
   *     that the unqualified annotation might stand for.
   * @return true if {@code suppressor} suppresses warnings regarding {@code suppressees}
   */
  static boolean suppresses(AnnotationExpr suppressor, Collection<String> suppressees) {
    List<String> suppressWarningsStrings = suppressWarningsStrings(suppressor);
    if (suppressWarningsStrings == null) {
      return false;
    }
    List<String> checkerNames =
        CollectionsPlume.mapList(
            RemoveAnnotationsForInference::checkerName, suppressWarningsStrings);
    // "allcheckers" suppresses all warnings.
    if (checkerNames.contains("allcheckers")) {
      return true;
    }

    // Try every element of suppressee's fully-qualified name.
    for (String suppressee : suppressees) {
      for (String fqPart : suppressee.split("\\.")) {
        if (checkerNames.contains(fqPart)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Given a @SuppressWarnings annotation, returns its strings. Given a different annotation that
   * suppresses warnings (e.g., @IgnoreInWholeProgramInference, @Inject, @Singleton), returns
   * strings for what it suppresses. Otherwise, returns null.
   *
   * @param n an annotation
   * @return the (effective) arguments to {@code @SuppressWarnings}, or null
   */
  private static @Nullable List<String> suppressWarningsStrings(AnnotationExpr n) {
    String name = n.getNameAsString();

    if (name.equals("SuppressWarnings") || name.equals("java.lang.SuppressWarnings")) {
      if (n instanceof MarkerAnnotationExpr) {
        return Collections.emptyList();
      } else if (n instanceof NormalAnnotationExpr) {
        NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) n).getPairs();
        assert pairs.size() == 1;
        MemberValuePair pair = pairs.get(0);
        assert pair.getName().asString().equals("value");
        return annotationElementStrings(pair.getValue());
      } else if (n instanceof SingleMemberAnnotationExpr) {
        return annotationElementStrings(((SingleMemberAnnotationExpr) n).getMemberValue());
      } else {
        throw new BugInCF("Unexpected AnnotationExpr of type %s: %s", n.getClass(), n);
      }
    }

    if (name.equals("IgnoreInWholeProgramInference")
        || name.equals("org.checkerframework.framework.qual.IgnoreInWholeProgramInference")
        || name.equals("Inject")
        || name.equals("javax.inject.Inject")
        || name.equals("Singleton")
        || name.equals("javax.inject.Singleton")
        || name.equals("Option")
        || name.equals("org.plumelib.options.Option")) {
      return Collections.singletonList("allcheckers");
    }

    return null;
  }

  /**
   * Given an annotation argument for an element of type String[], return a list of strings. Returns
   * null if the list of suppressed strings is unknown (e.g., if the argument is a name expression).
   *
   * @param e an annotation argument
   * @return the strings expressed by {@code e}
   */
  private static @Nullable List<String> annotationElementStrings(Expression e) {
    if (e instanceof StringLiteralExpr) {
      return Collections.singletonList(((StringLiteralExpr) e).asString());
    } else if (e instanceof ArrayInitializerExpr) {
      NodeList<Expression> values = ((ArrayInitializerExpr) e).getValues();
      List<String> result = new ArrayList<>(values.size());
      for (Expression v : values) {
        if (v instanceof StringLiteralExpr) {
          result.add(((StringLiteralExpr) v).asString());
        } else if (v instanceof NameExpr) {
          // TODO: is it better to return null here, thus causing nothing under this
          // warning to be treated as "suppressed", or to return any keys that are string
          // literals?  Returning null here ensures that if any argument to the SW
          // annotation isn't a string literal, then none of them are considered.
          return null;
        } else {
          throw new BugInCF("Unexpected annotation element of type %s: %s", v.getClass(), v);
        }
      }
      return result;
    } else if (e instanceof NameExpr) {
      // TODO: it would be better to check if the NameExpr represents a compile-time constant,
      // and, if so, to use its value. But, it's not possible to determine that from just the
      // result of the parser.
      return null;
    } else {
      throw new BugInCF("Unexpected %s: %s", e.getClass(), e);
    }
  }

  /**
   * Returns the "checker name" part of a SuppressWarnings string: the part before the colon, or the
   * whole thing if it contains no colon.
   *
   * @param s a SuppressWarnings string: the argument to {@code @SuppressWarnings}
   * @return the part of s before the colon, or the whole thing if it contains no colon
   */
  private static String checkerName(String s) {
    int colonPos = s.indexOf(":");
    if (colonPos == -1) {
      return s;
    } else {
      return s.substring(colonPos + 1);
    }
  }
}
