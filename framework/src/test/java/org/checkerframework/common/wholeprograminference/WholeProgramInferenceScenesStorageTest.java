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
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
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
          "  String aStringField;",
          "  StringBuffer aStringBufferField;",
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

  // The tests below exercise `isDeclaredDefaultFor`.  Two of the selectors of `@DefaultFor` are
  // evaluated here: `types` (both when the meta-annotation is read reflectively, as it is for a
  // supported qualifier, and when it is read from an Element, which requires handling
  // MirroredTypesException) and `names` (which cannot be evaluated, because no declaration name
  // is available).

  /** A qualifier that is the default for all uses of {@code String}. */
  @DefaultFor(types = String.class)
  @interface QualForString {}

  /** A qualifier that is the default for return types, and for declarations named "bottom". */
  @DefaultFor(value = TypeUseLocation.RETURN, names = ".*bottom.*")
  @interface QualForReturnAndNames {}

  /** A qualifier that is the default for declarations named "bottom". */
  @DefaultFor(names = ".*bottom.*")
  @interface QualForNames {}

  /**
   * A compilation unit that declares type qualifiers, so that their {@code @DefaultFor}
   * meta-annotations can be read from an {@link Element} rather than reflectively.
   */
  private static final String QUAL_SOURCE =
      String.join(
          System.lineSeparator(),
          "package testpkg;",
          "import org.checkerframework.framework.qual.DefaultFor;",
          "public class Outer {",
          "  @DefaultFor(types = String.class)",
          "  @interface EltQualForString {}",
          "  @DefaultFor(types = {Integer.class, StringBuffer.class})",
          "  @interface EltQualForTwoTypes {}",
          "  @DefaultFor(names = \".*bottom.*\")",
          "  @interface EltQualForNames {}",
          "}");

  /** Maps the name of each declaration in {@link #QUAL_SOURCE} to its element. */
  private static final Map<String, Element> qualElements = elementsOf(QUAL_SOURCE);

  /** Tests the {@code types} element of {@code @DefaultFor}, read reflectively. */
  @Test
  public void defaultForTypes() {
    DefaultFor defaultFor = QualForString.class.getAnnotation(DefaultFor.class);
    assertIsDeclaredDefaultFor(true, defaultFor, TypeUseLocation.FIELD, "aStringField");
    assertIsDeclaredDefaultFor(false, defaultFor, TypeUseLocation.FIELD, "aStringBufferField");
    assertIsDeclaredDefaultFor(false, defaultFor, TypeUseLocation.FIELD, "aField");
  }

  /**
   * Tests the {@code types} element of a {@code @DefaultFor} that was read from an {@link Element},
   * whose {@code Class}-valued element throws {@code MirroredTypesException} when read.
   */
  @Test
  public void defaultForTypesFromElement() {
    DefaultFor defaultFor = defaultForOf("EltQualForString");
    assertIsDeclaredDefaultFor(true, defaultFor, TypeUseLocation.FIELD, "aStringField");
    assertIsDeclaredDefaultFor(false, defaultFor, TypeUseLocation.FIELD, "aStringBufferField");
    assertIsDeclaredDefaultFor(false, defaultFor, TypeUseLocation.FIELD, "aField");

    DefaultFor twoTypes = defaultForOf("EltQualForTwoTypes");
    assertIsDeclaredDefaultFor(true, twoTypes, TypeUseLocation.FIELD, "aStringBufferField");
    assertIsDeclaredDefaultFor(false, twoTypes, TypeUseLocation.FIELD, "aStringField");
    // `int` is not `java.lang.Integer`.
    assertIsDeclaredDefaultFor(false, twoTypes, TypeUseLocation.FIELD, "aField");
  }

  /**
   * Tests that a {@code names} element that cannot be evaluated yields {@code
   * resultIfUndetermined}, whether the meta-annotation is read reflectively or from an {@link
   * Element}.
   */
  @Test
  public void defaultForNamesIsUndetermined() {
    for (DefaultFor defaultFor :
        new DefaultFor[] {
          QualForNames.class.getAnnotation(DefaultFor.class), defaultForOf("EltQualForNames")
        }) {
      assertIsDeclaredDefaultFor(true, defaultFor, TypeUseLocation.FIELD, "aStringField", true);
      assertIsDeclaredDefaultFor(false, defaultFor, TypeUseLocation.FIELD, "aStringField", false);
    }
  }

  /**
   * Tests that a selector that can be evaluated takes precedence over an unevaluatable {@code
   * names} element, and that a qualifier with no {@code @DefaultFor} match is not a default.
   */
  @Test
  public void evaluableSelectorsTakePrecedenceOverNames() {
    DefaultFor defaultFor = QualForReturnAndNames.class.getAnnotation(DefaultFor.class);
    // The `value` element matches, so the result does not depend on `resultIfUndetermined`.
    assertIsDeclaredDefaultFor(true, defaultFor, TypeUseLocation.RETURN, "aStringField", true);
    assertIsDeclaredDefaultFor(true, defaultFor, TypeUseLocation.RETURN, "aStringField", false);
    // The `value` element does not match, so `names` makes the result undetermined.
    assertIsDeclaredDefaultFor(true, defaultFor, TypeUseLocation.FIELD, "aStringField", true);
    assertIsDeclaredDefaultFor(false, defaultFor, TypeUseLocation.FIELD, "aStringField", false);
  }

  /** Tests that a qualifier with neither meta-annotation is not a declared default. */
  @Test
  public void noMetaAnnotationIsNotADefault() {
    Assert.assertFalse(
        WholeProgramInferenceScenesStorage.isDeclaredDefaultFor(
            null, null, TypeUseLocation.FIELD, typeOf("aStringField"), true));
  }

  /**
   * Returns the {@code @DefaultFor} meta-annotation of the qualifier that {@link #QUAL_SOURCE}
   * declares with name {@code qualName}.
   *
   * @param qualName the name of a qualifier declared in {@link #QUAL_SOURCE}
   * @return the {@code @DefaultFor} meta-annotation on that qualifier
   */
  private DefaultFor defaultForOf(String qualName) {
    Element element = qualElements.get(qualName);
    Assert.assertNotNull("no element named " + qualName, element);
    DefaultFor result = element.getAnnotation(DefaultFor.class);
    Assert.assertNotNull("no @DefaultFor on " + qualName, result);
    return result;
  }

  /**
   * Returns the type of the declaration in {@link #SOURCE} whose name is {@code elementName}.
   *
   * @param elementName the name of a declaration in {@link #SOURCE}
   * @return the type of that declaration
   */
  private TypeMirror typeOf(String elementName) {
    Element element = elements.get(elementName);
    Assert.assertNotNull("no element named " + elementName, element);
    return element.asType();
  }

  /**
   * Asserts that {@link WholeProgramInferenceScenesStorage#isDeclaredDefaultFor} returns {@code
   * expected}, for both values of its {@code resultIfUndetermined} parameter.
   *
   * @param expected the expected result
   * @param defaultFor the {@code @DefaultFor} meta-annotation to test
   * @param location the location where the qualifier would be written
   * @param elementName the name of the declaration in {@link #SOURCE} whose type is used
   */
  private void assertIsDeclaredDefaultFor(
      boolean expected,
      @Nullable DefaultFor defaultFor,
      TypeUseLocation location,
      String elementName) {
    assertIsDeclaredDefaultFor(expected, defaultFor, location, elementName, true);
    assertIsDeclaredDefaultFor(expected, defaultFor, location, elementName, false);
  }

  /**
   * Asserts that {@link WholeProgramInferenceScenesStorage#isDeclaredDefaultFor} returns {@code
   * expected}.
   *
   * @param expected the expected result
   * @param defaultFor the {@code @DefaultFor} meta-annotation to test
   * @param location the location where the qualifier would be written
   * @param elementName the name of the declaration in {@link #SOURCE} whose type is used
   * @param resultIfUndetermined what {@code isDeclaredDefaultFor} should return if it cannot
   *     evaluate a selector
   */
  private void assertIsDeclaredDefaultFor(
      boolean expected,
      @Nullable DefaultFor defaultFor,
      TypeUseLocation location,
      String elementName,
      boolean resultIfUndetermined) {
    Assert.assertEquals(
        "isDeclaredDefaultFor("
            + defaultFor
            + ", "
            + location
            + ", "
            + elementName
            + ", "
            + resultIfUndetermined
            + ")",
        expected,
        WholeProgramInferenceScenesStorage.isDeclaredDefaultFor(
            (DefaultQualifier) null,
            defaultFor,
            location,
            typeOf(elementName),
            resultIfUndetermined));
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
