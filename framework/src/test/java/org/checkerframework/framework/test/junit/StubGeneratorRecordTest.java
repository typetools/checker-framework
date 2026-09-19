package org.checkerframework.framework.test.junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
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
import org.checkerframework.framework.util.StaticJavaParserUtil;
import org.junit.Assert;
import org.junit.Test;

/** Tests that {@link StubGenerator} generates a parseable stub file for a record. */
public class StubGeneratorRecordTest {

  /** Creates a new StubGeneratorRecordTest. */
  public StubGeneratorRecordTest() {}

  @Test
  public void recordComponents() {
    String stub =
        generateStub(
            "p/Outer.java",
            "package p; public class Outer { public record Point(int x, String name) {} }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("record Outer$Point(int x, String name)"));
    // A record's superclass is java.lang.Record, which must not appear in an extends clause.
    Assert.assertFalse(stub, stub.contains("extends Record"));
    assertParses(stub);
  }

  @Test
  public void genericRecordComponents() {
    String stub =
        generateStub(
            "p/Outer.java",
            "package p;"
                + " public class Outer {"
                + "   public record Pair<K, V>(K key, V value) implements Cloneable {} }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("record Outer$Pair<K, V>(K key, V value)"));
    Assert.assertTrue(stub, stub.contains("implements Cloneable"));
    assertParses(stub);
  }

  @Test
  public void emptyRecordComponents() {
    String stub =
        generateStub(
            "p/Outer.java",
            "package p; public class Outer { public record Empty() {} }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("record Outer$Empty()"));
    assertParses(stub);
  }

  /**
   * Asserts that the given text is a parseable stub file.
   *
   * @param stub the text of a stub file
   */
  private void assertParses(String stub) {
    try {
      StaticJavaParserUtil.parseStubUnit(
          new ByteArrayInputStream(stub.getBytes(StandardCharsets.UTF_8)));
    } catch (RuntimeException e) {
      throw new AssertionError("Could not parse stub file:" + System.lineSeparator() + stub, e);
    }
  }

  /**
   * Runs {@link StubGenerator#stubFromType} on a type declared in the given source text.
   *
   * @param fileName the file name for the source text, such as {@code "p/Outer.java"}
   * @param source the text of a Java source file
   * @param typeName the canonical name of the type to generate a stub for; the type is declared in
   *     {@code source}
   * @return the generated stub file text
   */
  private String generateStub(String fileName, String source, String typeName) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    Assert.assertNotNull("No system Java compiler is available.", compiler);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StubGeneratorProcessor processor =
        new StubGeneratorProcessor(typeName, new PrintStream(baos, true, StandardCharsets.UTF_8));
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
    return baos.toString(StandardCharsets.UTF_8);
  }

  /** A Java source file whose contents are a string. */
  private static class SourceFile extends SimpleJavaFileObject {

    /** The contents of the source file. */
    private final String source;

    /**
     * Creates a new SourceFile.
     *
     * @param fileName the file name, such as {@code "p/Outer.java"}
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
        if (!(element instanceof TypeElement typeElement)) {
          continue;
        }
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
