package org.checkerframework.framework.test.junit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.framework.stub.StubGenerator;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link StubGenerator}. */
public class StubGeneratorTest {

  /** Matches a package declaration, which starts a line. */
  private static final Pattern packageDeclarationPattern =
      Pattern.compile("^[ \t]*package\\s", Pattern.MULTILINE);

  /** Creates a new StubGeneratorTest. */
  public StubGeneratorTest() {}

  @Test
  public void nestedClassInDefaultPackage() {
    String stub =
        generateStub("Foo.java", "public class Foo { public static class Inner {} }", "Foo.Inner");
    Assert.assertTrue(stub, stub.contains("class Foo$Inner"));
    assertNoPackageDeclaration(stub);
  }

  @Test
  public void doublyNestedClass() {
    String stub =
        generateStub(
            "p/Qux.java",
            "package p;"
                + " public class Qux { public static class Inner { public static class Innermost"
                + " {} } }",
            "p.Qux");
    Assert.assertTrue(stub, stub.contains("class Qux$Inner"));
    Assert.assertTrue(stub, stub.contains("class Qux$Inner$Innermost"));
  }

  @Test
  public void nestedClassInNamedPackage() {
    String stub =
        generateStub(
            "p/Bar.java",
            "package p; public class Bar { public static class Inner {} }",
            "p.Bar.Inner");
    Assert.assertTrue(stub, stub.contains("package p;"));
    Assert.assertTrue(stub, stub.contains("class Bar$Inner"));
  }

  @Test
  public void topLevelClassInDefaultPackage() {
    String stub = generateStub("Baz.java", "public class Baz {}", "Baz");
    Assert.assertTrue(stub, stub.contains("class Baz"));
    assertNoPackageDeclaration(stub);
  }

  /**
   * Asserts that the given stub file text contains no package declaration, as is correct for the
   * default package.
   *
   * @param stub the text of a stub file
   */
  private void assertNoPackageDeclaration(String stub) {
    Assert.assertFalse(stub, packageDeclarationPattern.matcher(stub).find());
  }

  /**
   * Runs {@link StubGenerator#stubFromType} on a type declared in the given source text.
   *
   * @param fileName the file name for the source text, such as {@code "p/Bar.java"}
   * @param source the text of a Java source file
   * @param typeName the canonical name of the type to generate a stub for; the type is declared in
   *     {@code source}
   * @return the generated stub file text
   */
  private String generateStub(String fileName, String source, String typeName) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    Assert.assertNotNull("No system Java compiler is available.", compiler);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
      StubGeneratorProcessor processor = new StubGeneratorProcessor(typeName, out);
      JavaCompiler.CompilationTask task =
          compiler.getTask(
              null,
              null,
              null,
              // "-proc:only" means do not generate class files.
              Arrays.asList("-proc:only"),
              null,
              Collections.singletonList(new SourceFile(fileName, source)));
      task.setProcessors(Collections.singletonList(processor));
      Assert.assertTrue("Compilation of " + fileName + " failed.", task.call());
      Assert.assertTrue("Did not find type " + typeName + ".", processor.foundType);
    }
    return baos.toString(StandardCharsets.UTF_8);
  }

  /** A Java source file whose contents are a string. */
  private static class SourceFile extends SimpleJavaFileObject {

    /** The contents of the source file. */
    private final String source;

    /**
     * Creates a new SourceFile.
     *
     * @param fileName the file name, such as {@code "p/Bar.java"}
     * @param source the contents of the source file
     */
    SourceFile(String fileName, String source) {
      super(URI.create("string:///" + fileName), JavaFileObject.Kind.SOURCE);
      this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return source;
    }
  }

  /** An annotation processor that generates a stub for one type. */
  @SupportedAnnotationTypes("*")
  private static class StubGeneratorProcessor extends AbstractProcessor {

    /** The canonical name of the type to generate a stub for. */
    private final String typeName;

    /** Where to write the stub. */
    private final PrintStream out;

    /** True if the type named {@link #typeName} was found. */
    boolean foundType = false;

    /**
     * Creates a new StubGeneratorProcessor.
     *
     * @param typeName the canonical name of the type to generate a stub for
     * @param out where to write the stub
     */
    StubGeneratorProcessor(String typeName, PrintStream out) {
      this.typeName = typeName;
      this.out = out;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      Deque<Element> worklist = new ArrayDeque<>(roundEnv.getRootElements());
      while (!worklist.isEmpty()) {
        Element element = worklist.remove();
        if (!(element instanceof TypeElement)) {
          continue;
        }
        TypeElement typeElement = (TypeElement) element;
        if (typeElement.getQualifiedName().contentEquals(typeName)) {
          foundType = true;
          new StubGenerator(out).stubFromType(typeElement);
        }
        worklist.addAll(typeElement.getEnclosedElements());
      }
      return false;
    }
  }
}
