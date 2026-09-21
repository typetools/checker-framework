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
import java.util.StringJoiner;
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

/** Methods that run {@link StubGenerator} on Java source text, for use by tests. */
public final class StubGeneratorTestHelper {

  /** This class cannot be instantiated. */
  private StubGeneratorTestHelper() {
    throw new AssertionError("Class StubGeneratorTestHelper cannot be instantiated.");
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
  public static String generateStub(String fileName, String source, String typeName) {
    return generateStub(typeName, new SourceFile(fileName, source));
  }

  /**
   * Runs {@link StubGenerator#stubFromType} on a type declared in the given source files.
   *
   * @param typeName the canonical name of the type to generate a stub for; the type is declared in
   *     one of {@code files}
   * @param files the Java source files to compile
   * @return the generated stub file text
   */
  public static String generateStub(String typeName, SourceFile... files) {
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
            Arrays.asList(files));
    task.setProcessors(Collections.singletonList(processor));
    Assert.assertTrue("Compilation of " + typeName + " failed.", task.call());
    Assert.assertTrue("Did not find type " + typeName + ".", processor.foundType);
    return baos.toString(StandardCharsets.UTF_8);
  }

  /**
   * Asserts that the given text is a parseable stub file.
   *
   * @param stub the text of a stub file
   */
  public static void assertParses(String stub) {
    try {
      StaticJavaParserUtil.parseStubUnit(
          new ByteArrayInputStream(stub.getBytes(StandardCharsets.UTF_8)));
    } catch (RuntimeException e) {
      throw new AssertionError("Could not parse stub file:" + System.lineSeparator() + stub, e);
    }
  }

  /**
   * Returns a source file that declares an annotation in package {@code p}.
   *
   * @param name the simple name of the annotation
   * @param targets the simple names of the {@code ElementType} constants that the annotation may be
   *     written on
   * @return a source file that declares the annotation
   */
  public static SourceFile annotationDeclaration(String name, String... targets) {
    StringJoiner targetList = new StringJoiner(", ", "{", "}");
    for (String target : targets) {
      targetList.add("java.lang.annotation.ElementType." + target);
    }
    return new SourceFile(
        "p/" + name + ".java",
        "package p;"
            + " @java.lang.annotation.Target("
            + targetList
            + ")"
            + " public @interface "
            + name
            + " {}");
  }

  /** A Java source file whose contents are a string. */
  public static class SourceFile extends SimpleJavaFileObject {

    /** The contents of the source file. */
    private final String source;

    /**
     * Creates a new SourceFile.
     *
     * @param fileName the file name, such as {@code "p/Outer.java"}
     * @param source the contents of the source file
     */
    public SourceFile(String fileName, String source) {
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
