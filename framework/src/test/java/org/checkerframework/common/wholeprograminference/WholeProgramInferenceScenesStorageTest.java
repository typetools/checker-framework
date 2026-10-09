package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link WholeProgramInferenceScenesStorage}. */
public class WholeProgramInferenceScenesStorageTest {

  /** The compilation unit whose elements the tests use. */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "package testpkg;",
          "@Deprecated",
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
          "  String aliasedMethod() {",
          "    return \"x\";",
          "  }",
          "  String unaliasedMethod() {",
          "    return \"x\";",
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

  /**
   * A checker, needed to construct a {@link WholeProgramInferenceScenesStorage} and to write out
   * its results. Writing a Scene that contains a method consults {@code checker.getTypeFactory()},
   * to compute the method's contracts, so the checker must be initialized rather than merely
   * constructed. {@code ValueChecker} is a concrete checker that the framework tests already depend
   * on; any other concrete checker would do.
   */
  private static final ValueChecker checker = new ValueChecker();

  /** The type factory of {@link #checker}. */
  private static final AnnotatedTypeFactory typeFactory;

  static {
    Context context = new Context();
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    com.sun.tools.javac.main.JavaCompiler javac =
        com.sun.tools.javac.main.JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    checker.init(env);
    checker.initChecker();
    typeFactory = checker.getTypeFactory();
  }

  /** The directory that whole-program inference writes its results into. */
  @Rule public TemporaryFolder outputDirectory = new TemporaryFolder();

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
   * A file is written out only if a Scene was created for it. {@code setFileModified} enforces that
   * invariant, on which {@code writeResultsToFile} depends.
   */
  @Test
  public void setFileModifiedForFileWithoutScene() {
    WholeProgramInferenceScenesStorage storage = newStorage();
    storage.setFileModified(outputDirectoryPath().resolve("testpkg.Unknown.jaif").toString());
    storage.writeResultsToFile(OutputFormat.JAIF, checker);
    Assert.assertArrayEquals(
        "wrote a file for a class with no Scene", new String[0], outputDirectory.getRoot().list());
  }

  /** A file is written out if a Scene was created for it and it was marked as modified. */
  @Test
  public void setFileModifiedForFileWithScene() {
    WholeProgramInferenceScenesStorage storage = newStorage();
    TypeElement outer = (TypeElement) elements.get("Outer");
    Assert.assertNotNull("no element named Outer", outer);
    Assert.assertTrue(
        "did not add @Deprecated to the Scene for testpkg.Outer",
        storage.addClassDeclarationAnnotation(outer, deprecatedAnnotation(outer)));
    storage.setFileModified(storage.getFileForElement(outer));
    storage.writeResultsToFile(OutputFormat.JAIF, checker);
    Assert.assertArrayEquals(
        "did not write the .jaif file for testpkg.Outer",
        new String[] {"testpkg.Outer.jaif"},
        outputDirectory.getRoot().list());
  }

  /**
   * Writing out a Scene that contains a method consults the checker's type factory, to compute the
   * method's contracts.
   */
  @Test
  public void writeSceneContainingMethod() throws IOException {
    WholeProgramInferenceScenesStorage storage = newStorage();
    ExecutableElement aMethod = (ExecutableElement) elements.get("aMethod");
    TypeElement outer = (TypeElement) elements.get("Outer");
    Assert.assertNotNull("no element named aMethod", aMethod);
    Assert.assertNotNull("no element named Outer", outer);
    Assert.assertTrue(
        "did not add @Deprecated to the Scene for aMethod",
        storage.addMethodDeclarationAnnotation(aMethod, deprecatedAnnotation(outer)));
    storage.setFileModified(storage.getFileForElement(aMethod));
    storage.writeResultsToFile(OutputFormat.JAIF, checker);
    Path jaifFile = outputDirectoryPath().resolve("testpkg.Outer.jaif");
    Assert.assertTrue("did not write " + jaifFile, Files.exists(jaifFile));
    String jaifContents = new String(Files.readAllBytes(jaifFile), StandardCharsets.UTF_8);
    Assert.assertTrue(
        "@Deprecated is not on aMethod in "
            + jaifFile
            + ":"
            + System.lineSeparator()
            + jaifContents,
        jaifContents.contains("method aMethod(I)V: @java.lang.Deprecated"));
  }

  /**
   * An inferred annotation that is redundant in the source code is not written to the output file.
   * This test is the baseline for {@link #aliasedMethodDeclarationAnnotationPreservesReturnType}.
   */
  @Test
  public void redundantReturnTypeAnnotationIsNotWritten() throws IOException {
    WholeProgramInferenceScenesStorage storage = newStorage();
    ExecutableElement unaliasedMethod = methodNamed("unaliasedMethod");
    inferRedundantReturnType(storage, unaliasedMethod);
    String jaifContents = writeJaif(storage, unaliasedMethod);
    Assert.assertTrue(
        "unaliasedMethod is absent from the .jaif file:" + System.lineSeparator() + jaifContents,
        jaifContents.contains("method unaliasedMethod()"));
    Assert.assertFalse(
        "the redundant @StringVal was written to the .jaif file:"
            + System.lineSeparator()
            + jaifContents,
        jaifContents.contains("StringVal"));
  }

  /**
   * Writing, on a method, a declaration annotation that is an alias for a type qualifier gives the
   * method's return type that qualifier. Therefore, an annotation that the method's return type has
   * in the input program is not redundant in the output file, even if whole-program inference would
   * ordinarily omit it.
   */
  @Test
  public void aliasedMethodDeclarationAnnotationPreservesReturnType() throws IOException {
    WholeProgramInferenceScenesStorage storage = newStorage();
    ExecutableElement aliasedMethod = methodNamed("aliasedMethod");
    AnnotationMirror minLen = minLenAnnotation();
    Assert.assertFalse(
        "@MinLen is not an alias for a qualifier of the Constant Value Checker",
        AnnotationUtils.areSameByName(typeFactory.canonicalAnnotation(minLen), minLen));
    Assert.assertTrue(
        "did not add @MinLen to the Scene for aliasedMethod",
        storage.addMethodDeclarationAnnotation(aliasedMethod, minLen));
    inferRedundantReturnType(storage, aliasedMethod);
    String jaifContents = writeJaif(storage, aliasedMethod);
    Assert.assertTrue(
        "@StringVal is not on the return type of aliasedMethod in the .jaif file:"
            + System.lineSeparator()
            + jaifContents,
        jaifContents.contains("StringVal"));
  }

  /**
   * Returns the method in {@link #SOURCE} whose name is {@code methodName}.
   *
   * @param methodName the name of a method declared in {@link #SOURCE}
   * @return the element for that method
   */
  private static ExecutableElement methodNamed(String methodName) {
    Element result = elements.get(methodName);
    Assert.assertNotNull("no element named " + methodName, result);
    return (ExecutableElement) result;
  }

  /**
   * Infers, for the return type of {@code methodElt}, an annotation that the source code already
   * has. Whole-program inference does not write such an annotation to the output file, unless some
   * other inference for {@code methodElt} makes it non-redundant.
   *
   * @param storage the storage to record the inference in
   * @param methodElt a method whose return type is {@code String}
   */
  private void inferRedundantReturnType(
      WholeProgramInferenceScenesStorage storage, ExecutableElement methodElt) {
    AnnotationMirror stringVal = stringValAnnotation();
    AnnotatedTypeMirror inferredType = annotatedStringType(stringVal);
    AnnotatedTypeMirror declaredType = annotatedStringType(stringVal);
    storage.updateStorageLocationFromAtm(
        inferredType,
        declaredType,
        storage.getReturnAnnotations(methodElt, inferredType, typeFactory),
        TypeUseLocation.RETURN,
        false);
  }

  /**
   * Writes out the scene that contains {@code methodElt}, and returns the contents of the .jaif
   * file that was written.
   *
   * @param storage the storage to write out
   * @param methodElt a method whose scene has been modified
   * @return the contents of the .jaif file for {@code methodElt}'s class
   */
  private String writeJaif(WholeProgramInferenceScenesStorage storage, ExecutableElement methodElt)
      throws IOException {
    storage.setFileModified(storage.getFileForElement(methodElt));
    storage.writeResultsToFile(OutputFormat.JAIF, checker);
    Path jaifFile = outputDirectoryPath().resolve("testpkg.Outer.jaif");
    Assert.assertTrue("did not write " + jaifFile, Files.exists(jaifFile));
    return new String(Files.readAllBytes(jaifFile), StandardCharsets.UTF_8);
  }

  /**
   * Returns the type {@code String}, with {@code anno} as its primary annotation.
   *
   * @param anno a type qualifier
   * @return the type {@code String} annotated with {@code anno}
   */
  private AnnotatedTypeMirror annotatedStringType(AnnotationMirror anno) {
    AnnotatedTypeMirror result =
        AnnotatedTypeMirror.createType(typeOf("aStringField"), typeFactory, false);
    result.addAnnotation(anno);
    return result;
  }

  /**
   * Returns a {@code @MinLen} annotation, which the Constant Value Checker treats as an alias for
   * the type qualifier {@code @ArrayLenRange}. {@link
   * WholeProgramInferenceScenesStorage#addMethodDeclarationAnnotation} does not check where its
   * argument may be written, only whether it is an alias for a type qualifier.
   *
   * @return a {@code @MinLen} annotation
   */
  private static AnnotationMirror minLenAnnotation() {
    return AnnotationBuilder.fromClass(typeFactory.getElementUtils(), MinLen.class);
  }

  /**
   * Returns a {@code @StringVal("x")} annotation, which is a type qualifier of the Constant Value
   * Checker.
   *
   * @return a {@code @StringVal("x")} annotation
   */
  private static AnnotationMirror stringValAnnotation() {
    AnnotationBuilder builder =
        new AnnotationBuilder(typeFactory.getProcessingEnv(), StringVal.class);
    builder.setValue("value", new String[] {"x"});
    return builder.build();
  }

  /**
   * Creates a storage that writes its results into {@link #outputDirectory}.
   *
   * @return a new storage that writes its results into {@link #outputDirectory}
   */
  private WholeProgramInferenceScenesStorage newStorage() {
    return new WholeProgramInferenceScenesStorage(
        typeFactory, outputDirectory.getRoot().toString());
  }

  /**
   * Returns {@link #outputDirectory} as a path.
   *
   * @return {@link #outputDirectory} as a path
   */
  private Path outputDirectoryPath() {
    return outputDirectory.getRoot().toPath();
  }

  /**
   * Returns the {@code @Deprecated} annotation on {@code element}.
   *
   * @param element an element that is declared with a {@code @Deprecated} annotation
   * @return the {@code @Deprecated} annotation on {@code element}
   */
  private static AnnotationMirror deprecatedAnnotation(Element element) {
    for (AnnotationMirror anno : element.getAnnotationMirrors()) {
      if (AnnotationUtils.areSameByName(anno, Deprecated.class.getCanonicalName())) {
        return anno;
      }
    }
    throw new AssertionError(element + " is not annotated with @Deprecated");
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
            // The URI scheme must be "file:", so that ElementUtils.isElementFromSourceCode
            // returns true for the elements of the compilation unit.
            URI.create("file:///testpkg/Outer.java"), JavaFileObject.Kind.SOURCE) {
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
