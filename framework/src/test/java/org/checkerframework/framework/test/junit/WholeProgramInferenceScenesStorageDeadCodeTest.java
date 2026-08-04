package org.checkerframework.framework.test.junit;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.checkerframework.framework.util.JavaParserUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@code WholeProgramInferenceScenesStorage} contains no dead method: every method that
 * it declares is reachable from a method that overrides a supertype's method, from a call in
 * another class, or from an initializer or a constructor of {@code
 * WholeProgramInferenceScenesStorage}. A method that is called only by a dead method is dead.
 *
 * <p>The test recognizes a call by its method name only, so it does not detect a dead method whose
 * name is also the name of a method that is called in some other class.
 */
public class WholeProgramInferenceScenesStorageDeadCodeTest {

  /** The file whose methods must all be reachable, relative to the repository root. */
  private static final String fileUnderTest =
      "framework/src/main/java/org/checkerframework/common/wholeprograminference"
          + "/WholeProgramInferenceScenesStorage.java";

  /** Tests that every method in {@link #fileUnderTest} is reachable. */
  @Test
  public void noDeadMethods() throws IOException {
    Path repositoryRoot = repositoryRoot();
    Path file = repositoryRoot.resolve(fileUnderTest);
    Assert.assertTrue("No such file: " + file, Files.exists(file));
    CompilationUnit compilationUnit = parse(file);

    // Maps the name of a method declared in the file to the names of the methods that it calls.
    Map<String, Set<String>> calls = new LinkedHashMap<>();
    // The methods that are reachable without being called by another method in the file.
    Set<String> roots = new LinkedHashSet<>();
    for (MethodDeclaration method : compilationUnit.findAll(MethodDeclaration.class)) {
      String name = method.getNameAsString();
      calls.computeIfAbsent(name, k -> new LinkedHashSet<>()).addAll(calledNames(method));
      if (method.isAnnotationPresent("Override")) {
        roots.add(name);
      }
    }
    // A call outside every method declaration is in a constructor or an initializer, which is
    // always executed.
    for (Node call : callNodes(compilationUnit)) {
      if (!isWithinMethodDeclaration(call)) {
        roots.add(calledName(call));
      }
    }
    roots.addAll(namesCalledElsewhere(repositoryRoot, file, calls.keySet()));

    Set<String> reachable = new LinkedHashSet<>();
    Deque<String> worklist = new ArrayDeque<>(roots);
    while (!worklist.isEmpty()) {
      String name = worklist.pop();
      if (calls.containsKey(name) && reachable.add(name)) {
        worklist.addAll(calls.get(name));
      }
    }

    Set<String> dead = new LinkedHashSet<>(calls.keySet());
    dead.removeAll(reachable);
    Assert.assertTrue(
        "Dead code: these methods of " + fileUnderTest + " are never called: " + dead,
        dead.isEmpty());
  }

  /**
   * Returns those of the given method names that are called in a Java file other than {@code
   * fileUnderTest}.
   *
   * @param repositoryRoot the root directory of the checker-framework repository
   * @param fileUnderTest the file whose methods are being tested
   * @param names the method names to search for
   * @return the names that are called elsewhere in the repository
   * @throws IOException if a file cannot be read
   */
  private static Set<String> namesCalledElsewhere(
      Path repositoryRoot, Path fileUnderTest, Set<String> names) throws IOException {
    Set<String> result = new LinkedHashSet<>();
    Set<String> notYetFound = new LinkedHashSet<>(names);
    for (Path javaFile : javaSourceFiles(repositoryRoot)) {
      if (notYetFound.isEmpty()) {
        break;
      }
      if (javaFile.equals(fileUnderTest)) {
        continue;
      }
      // Reading as ISO 8859-1 never fails and preserves ASCII, which is all that searching for a
      // method name requires.  Parsing every file would be much slower.
      String contents = new String(Files.readAllBytes(javaFile), StandardCharsets.ISO_8859_1);
      if (notYetFound.stream().noneMatch(contents::contains)) {
        continue;
      }
      for (String called : calledNames(parse(javaFile))) {
        if (notYetFound.remove(called)) {
          result.add(called);
        }
      }
    }
    return result;
  }

  /**
   * Returns the names of the methods that are called within the given AST node.
   *
   * @param node an AST node
   * @return the names of the methods that are called within the node
   */
  private static Set<String> calledNames(Node node) {
    Set<String> result = new LinkedHashSet<>();
    for (Node call : callNodes(node)) {
      result.add(calledName(call));
    }
    return result;
  }

  /**
   * Returns the method calls and method references within the given AST node.
   *
   * @param node an AST node
   * @return the method calls and method references within the node
   */
  private static List<Node> callNodes(Node node) {
    List<Node> result = new ArrayList<>();
    result.addAll(node.findAll(MethodCallExpr.class));
    result.addAll(node.findAll(MethodReferenceExpr.class));
    return result;
  }

  /**
   * Returns true if the given AST node is within a method declaration.
   *
   * @param node an AST node
   * @return true if the node is within a method declaration
   */
  private static boolean isWithinMethodDeclaration(Node node) {
    for (Node n = node; n != null; n = n.getParentNode().orElse(null)) {
      if (n instanceof MethodDeclaration) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the name of the method that the given call or method reference names.
   *
   * @param call a {@code MethodCallExpr} or a {@code MethodReferenceExpr}
   * @return the name of the called method
   */
  private static String calledName(Node call) {
    if (call instanceof MethodCallExpr methodCall) {
      return methodCall.getNameAsString();
    } else {
      return ((MethodReferenceExpr) call).getIdentifier();
    }
  }

  /**
   * Returns the root directory of the checker-framework repository.
   *
   * @return the root directory of the checker-framework repository
   */
  private static Path repositoryRoot() {
    Path currentDirectory = Path.of("").toAbsolutePath();
    for (Path dir = currentDirectory; dir != null; dir = dir.getParent()) {
      if (Files.exists(dir.resolve("settings.gradle"))) {
        return dir;
      }
    }
    throw new AssertionError("No repository root at or above " + currentDirectory);
  }

  /**
   * Returns every .java file in a source directory of a subproject of the repository.
   *
   * @param repositoryRoot the root directory of the checker-framework repository
   * @return the .java files that might contain a call
   * @throws IOException if a directory cannot be read
   */
  private static List<Path> javaSourceFiles(Path repositoryRoot) throws IOException {
    List<Path> result = new ArrayList<>();
    try (Stream<Path> subprojects = Files.list(repositoryRoot)) {
      for (Path subprojectDir : subprojects.toList()) {
        Path srcDir = subprojectDir.resolve("src");
        if (Files.isDirectory(srcDir)) {
          try (Stream<Path> files = Files.walk(srcDir)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(result::add);
          }
        }
      }
    }
    return result;
  }

  /**
   * Parses the given Java file.
   *
   * @param file a Java file
   * @return the parsed file
   */
  private static CompilationUnit parse(Path file) {
    try {
      return JavaParserUtil.parseCompilationUnit(file.toFile());
    } catch (FileNotFoundException e) {
      throw new AssertionError(e);
    }
  }
}
