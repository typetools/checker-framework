package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link WholeProgramInferenceScenesStorage}. */
public class WholeProgramInferenceScenesStorageTest {

  /** The compilation unit whose elements the tests use. */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "package testpkg;",
          "public class Outer {",
          "  int aField;",
          "  Outer(int ctorParam) {",
          "    int ctorLocal = ctorParam;",
          "  }",
          "  void aMethod(int methodParam) {",
          "    int methodLocal = methodParam;",
          "  }",
          "  static class Inner {",
          "    void innerMethod() {",
          "      int innerLocal = 0;",
          "    }",
          "  }",
          "  enum AnEnum {",
          "    CONST;",
          "  }",
          "}");

  /** Maps the name of each declaration in {@link #SOURCE} to its element. */
  private static final Map<String, Element> elements = elementsOf(SOURCE);

  @Test
  public void classes() {
    assertEnclosingClassName("testpkg.Outer", "Outer", ElementKind.CLASS);
    assertEnclosingClassName("testpkg.Outer$Inner", "Inner", ElementKind.CLASS);
  }

  @Test
  public void methodsAndConstructors() {
    assertEnclosingClassName("testpkg.Outer", "Outer.<init>", ElementKind.CONSTRUCTOR);
    assertEnclosingClassName("testpkg.Outer$Inner", "Inner.<init>", ElementKind.CONSTRUCTOR);
    assertEnclosingClassName("testpkg.Outer", "aMethod", ElementKind.METHOD);
    assertEnclosingClassName("testpkg.Outer$Inner", "innerMethod", ElementKind.METHOD);
  }

  @Test
  public void fieldsAndParameters() {
    assertEnclosingClassName("testpkg.Outer", "aField", ElementKind.FIELD);
    assertEnclosingClassName("testpkg.Outer$AnEnum", "CONST", ElementKind.ENUM_CONSTANT);
    assertEnclosingClassName("testpkg.Outer", "ctorParam", ElementKind.PARAMETER);
    assertEnclosingClassName("testpkg.Outer", "methodParam", ElementKind.PARAMETER);
  }

  @Test
  public void localVariables() {
    assertEnclosingClassName("testpkg.Outer", "ctorLocal", ElementKind.LOCAL_VARIABLE);
    assertEnclosingClassName("testpkg.Outer", "methodLocal", ElementKind.LOCAL_VARIABLE);
    assertEnclosingClassName("testpkg.Outer$Inner", "innerLocal", ElementKind.LOCAL_VARIABLE);
  }

  /**
   * Tests that {@link #elementsOf} reports a compilation error in its argument, rather than
   * silently returning the elements that javac managed to create.
   */
  @Test
  public void uncompilableSourceIsReported() {
    // The class must be named "Outer", to match the file name that `elementsOf` uses, so that the
    // type error is the only compilation error.
    String uncompilableSource =
        String.join(
            System.lineSeparator(),
            "package testpkg;",
            "public class Outer {",
            "  int aField = \"not an int\";",
            "}");
    try {
      elementsOf(uncompilableSource);
    } catch (Error e) {
      String message = e.getMessage();
      Assert.assertTrue("unexpected message: " + message, message.startsWith("Cannot compile "));
      Assert.assertTrue("unexpected message: " + message, message.contains("incompatible types"));
      return;
    }
    Assert.fail("elementsOf did not report the compilation error in " + uncompilableSource);
  }

  /**
   * Asserts that {@link WholeProgramInferenceScenesStorage#getEnclosingClassName} returns {@code
   * expectedName} for the element of {@link #SOURCE} that is declared with name {@code
   * elementName}.
   *
   * @param expectedName the expected binary name of the enclosing class
   * @param elementName the name of a declaration in {@link #SOURCE}
   * @param expectedKind the expected kind of the element named {@code elementName}
   */
  private void assertEnclosingClassName(
      String expectedName, String elementName, ElementKind expectedKind) {
    Element element = elements.get(elementName);
    Assert.assertNotNull("no element named " + elementName, element);
    Assert.assertEquals("kind of " + elementName, expectedKind, element.getKind());
    Assert.assertEquals(
        "enclosing class of " + elementName,
        expectedName,
        WholeProgramInferenceScenesStorage.getEnclosingClassName(element));
  }

  /**
   * Compiles {@code source} and returns the element of every class, method, and variable that it
   * declares, indexed by the name of the declaration. The declarations in {@code source} must have
   * distinct names.
   *
   * @param source the text of a Java compilation unit
   * @return the elements declared in {@code source}, indexed by name
   * @throws Error if {@code source} does not compile without errors
   */
  private static Map<String, Element> elementsOf(String source) {
    JavaFileObject fileObject =
        new SimpleJavaFileObject(
            URI.create("string:///testpkg/Outer.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
          }
        };
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavacTask task =
        (JavacTask)
            compiler.getTask(
                null,
                null,
                diagnostics,
                Collections.singletonList("-proc:none"),
                null,
                Collections.singletonList(fileObject));
    Iterable<? extends CompilationUnitTree> compilationUnits;
    try {
      compilationUnits = task.parse();
      task.analyze();
    } catch (IOException e) {
      throw new Error("Cannot compile " + source, e);
    }
    // Without this check, a compilation error in `source` would be reported only as a missing
    // element, which is much harder to diagnose.
    StringBuilder errors = new StringBuilder();
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
        errors.append(System.lineSeparator());
        errors.append(diagnostic);
      }
    }
    if (errors.length() != 0) {
      throw new Error("Cannot compile " + source + errors);
    }
    Trees trees = Trees.instance(task);
    Map<String, Element> result = new LinkedHashMap<>();
    TreePathScanner<Void, Void> scanner =
        new TreePathScanner<Void, Void>() {
          @Override
          public Void visitClass(ClassTree tree, Void p) {
            recordElement();
            return super.visitClass(tree, p);
          }

          @Override
          public Void visitMethod(MethodTree tree, Void p) {
            recordElement();
            return super.visitMethod(tree, p);
          }

          @Override
          public Void visitVariable(VariableTree tree, Void p) {
            recordElement();
            return super.visitVariable(tree, p);
          }

          /** Adds the element at the current path to {@code result}. */
          private void recordElement() {
            Element element = trees.getElement(getCurrentPath());
            if (element == null) {
              return;
            }
            // Every class has a constructor, so qualify each constructor by its class name.
            String key =
                element.getKind() == ElementKind.CONSTRUCTOR
                    ? element.getEnclosingElement().getSimpleName() + ".<init>"
                    : element.getSimpleName().toString();
            Element previous = result.put(key, element);
            if (previous != null) {
              throw new Error("Duplicate declaration name " + key);
            }
          }
        };
    for (CompilationUnitTree compilationUnit : compilationUnits) {
      scanner.scan(new TreePath(compilationUnit), null);
    }
    return result;
  }
}
