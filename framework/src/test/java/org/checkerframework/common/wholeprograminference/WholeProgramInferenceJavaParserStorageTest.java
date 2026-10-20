package org.checkerframework.common.wholeprograminference;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceJavaParserStorage.FieldAnnos;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link WholeProgramInferenceJavaParserStorage}. */
public class WholeProgramInferenceJavaParserStorageTest {

  /** A lonely high surrogate character, as a string. */
  private static final String HIGH_SURROGATE = String.valueOf((char) 0xD83D);

  /** A lonely low surrogate character, as a string. */
  private static final String LOW_SURROGATE = String.valueOf((char) 0xDE00);

  /** The surrogate pair that represents the character U+1F600. */
  private static final String SURROGATE_PAIR = HIGH_SURROGATE + LOW_SURROGATE;

  /** Creates a new WholeProgramInferenceJavaParserStorageTest. */
  public WholeProgramInferenceJavaParserStorageTest() {}

  /** Tests {@link WholeProgramInferenceJavaParserStorage#packageNameToDirectory}. */
  @Test
  public void testPackageNameToDirectory() {
    Assert.assertEquals(
        "org/checkerframework",
        WholeProgramInferenceJavaParserStorage.packageNameToDirectory("org.checkerframework", '/'));
    Assert.assertEquals(
        "org", WholeProgramInferenceJavaParserStorage.packageNameToDirectory("org", '/'));
    // The separator is used literally, even when it is a Windows backslash.
    Assert.assertEquals(
        "org\\checkerframework",
        WholeProgramInferenceJavaParserStorage.packageNameToDirectory(
            "org.checkerframework", '\\'));
    Assert.assertEquals(
        "org", WholeProgramInferenceJavaParserStorage.packageNameToDirectory("org", '\\'));
  }

  /**
   * Tests that {@link FieldAnnos#transferAnnotations} reports a {@link BugInCF}, rather than
   * silently discarding the inferred annotations, when the wrapped variable declarator has no
   * parent node.
   */
  @Test
  public void testTransferAnnotationsWithoutParent() {
    VariableDeclarator declaration = new VariableDeclarator(PrimitiveType.intType(), "f");
    Assert.assertFalse(declaration.getParentNode().isPresent());

    BugInCF exception = assertTransferAnnotationsThrows(declaration);
    Assert.assertTrue(exception.getMessage(), exception.getMessage().contains("found null [null]"));
    // The declarator is left entirely unchanged.
    Assert.assertFalse(declaration.getParentNode().isPresent());
    Assert.assertEquals("int", declaration.getTypeAsString());
    Assert.assertEquals("f", declaration.getNameAsString());
    Assert.assertTrue(declaration.getType().getAnnotations().isEmpty());
  }

  /**
   * Tests that {@link FieldAnnos#transferAnnotations} reports a {@link BugInCF}, rather than
   * silently discarding the inferred annotations, when the wrapped variable declarator's parent is
   * not a {@code FieldDeclaration}.
   */
  @Test
  public void testTransferAnnotationsWithNonFieldParent() {
    // A local variable declaration, whose declarator's parent is a VariableDeclarationExpr.
    VariableDeclarator declaration =
        StaticJavaParser.parseStatement("int f;")
            .asExpressionStmt()
            .getExpression()
            .asVariableDeclarationExpr()
            .getVariable(0);
    Node declarationParent = declaration.getParentNode().orElse(null);
    Assert.assertTrue(declarationParent instanceof VariableDeclarationExpr);

    BugInCF exception = assertTransferAnnotationsThrows(declaration);
    Assert.assertTrue(
        exception.getMessage(), exception.getMessage().contains("VariableDeclarationExpr"));
    Assert.assertTrue(declaration.getType().getAnnotations().isEmpty());
  }

  /**
   * Asserts that {@link FieldAnnos#transferAnnotations} throws {@link BugInCF} for a {@link
   * FieldAnnos} that wraps {@code declaration} and that has an inferred type.
   *
   * @param declaration the variable declarator to wrap
   * @return the thrown exception
   */
  private static BugInCF assertTransferAnnotationsThrows(VariableDeclarator declaration) {
    FieldAnnos fieldAnnos = new FieldAnnos(declaration);
    // Initialize the inferred type, so that transferAnnotations() would attempt a real transfer
    // if it did not stop at the unexpected parent node.
    AnnotatedTypeMirror intType =
        AnnotatedTypeMirror.createType(
            env.getTypeUtils().getPrimitiveType(TypeKind.INT), typeFactory, false);
    fieldAnnos.getType(intType, typeFactory);

    return Assert.assertThrows(BugInCF.class, fieldAnnos::transferAnnotations);
  }

  /**
   * Tests that {@link CallableDeclarationAnnos#toString} shows every kind of inferred state,
   * including both the preconditions and the postconditions.
   */
  @Test
  public void testCallableDeclarationAnnosToString() {
    MethodDeclaration methodDeclaration =
        StaticJavaParser.parseMethodDeclaration("void aMethod() {}");
    CallableDeclarationAnnos methodAnnos =
        storage.new CallableDeclarationAnnos("testpkg.Outer", methodDeclaration);
    AnnotatedTypeMirror stringType =
        AnnotatedTypeMirror.createType(
            env.getElementUtils().getTypeElement("java.lang.String").asType(), typeFactory, false);
    methodAnnos.getPreOrPostconditionsForExpression(
        Analysis.BeforeOrAfter.BEFORE,
        "testpkg.Outer",
        "aMethod",
        "this.aPreconditionField",
        stringType,
        typeFactory);
    methodAnnos.getPreOrPostconditionsForExpression(
        Analysis.BeforeOrAfter.AFTER,
        "testpkg.Outer",
        "aMethod",
        "this.aPostconditionField",
        stringType,
        typeFactory);

    String methodAnnosString = methodAnnos.toString();
    Assert.assertTrue(methodAnnosString, methodAnnosString.contains("testpkg.Outer.aMethod"));
    Assert.assertTrue(methodAnnosString, methodAnnosString.contains("this.aPreconditionField"));
    Assert.assertTrue(methodAnnosString, methodAnnosString.contains("this.aPostconditionField"));
  }

  /** The processing environment for {@link #typeFactory}. */
  private static final ProcessingEnvironment env;

  /** Creates the annotated types that the tests use. */
  private static final AnnotatedTypeFactory typeFactory;

  /** The storage that owns the {@link CallableDeclarationAnnos} that the tests use. */
  private static final WholeProgramInferenceJavaParserStorage storage;

  static {
    Context context = new Context();
    env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    // Any concrete checker would do; ValueChecker is one that the framework tests already depend
    // on.
    ValueChecker checker = new ValueChecker();
    checker.init(env);
    typeFactory = new AnnotatedTypeFactory(checker);
    // The tests never write any file, so the output directory is never created.
    storage = new WholeProgramInferenceJavaParserStorage(typeFactory, "build/wpi-test", false);
  }

  /** Tests {@link WholeProgramInferenceJavaParserStorage#indexOfLonelySurrogateCharacter}. */
  @Test
  public void testIndexOfLonelySurrogateCharacter() {
    assertIndexOfLonelySurrogateCharacter(-1, "");
    assertIndexOfLonelySurrogateCharacter(-1, "abc");
    assertIndexOfLonelySurrogateCharacter(-1, SURROGATE_PAIR);
    assertIndexOfLonelySurrogateCharacter(-1, "a" + SURROGATE_PAIR + "b");
    assertIndexOfLonelySurrogateCharacter(0, HIGH_SURROGATE);
    assertIndexOfLonelySurrogateCharacter(0, LOW_SURROGATE);
    assertIndexOfLonelySurrogateCharacter(1, "a" + HIGH_SURROGATE);
    assertIndexOfLonelySurrogateCharacter(1, "a" + LOW_SURROGATE + "b");
    // The first character is lonely even though a surrogate pair immediately follows it.
    assertIndexOfLonelySurrogateCharacter(0, HIGH_SURROGATE + SURROGATE_PAIR);
    // The lonely character follows a surrogate pair, which is not itself lonely.
    assertIndexOfLonelySurrogateCharacter(2, SURROGATE_PAIR + LOW_SURROGATE);
  }

  /** Tests {@link WholeProgramInferenceJavaParserStorage#escapeLonelySurrogates}. */
  @Test
  public void testEscapeLonelySurrogates() {
    assertEscapeLonelySurrogates("", "");
    assertEscapeLonelySurrogates("abc", "abc");
    assertEscapeLonelySurrogates(SURROGATE_PAIR, SURROGATE_PAIR);
    assertEscapeLonelySurrogates("a" + SURROGATE_PAIR + "b", "a" + SURROGATE_PAIR + "b");
    assertEscapeLonelySurrogates("\\uD83D", HIGH_SURROGATE);
    assertEscapeLonelySurrogates("a\\uDE00b", "a" + LOW_SURROGATE + "b");
    // Every lonely surrogate character is escaped, and no other character is.
    assertEscapeLonelySurrogates("\\uD83D" + SURROGATE_PAIR, HIGH_SURROGATE + SURROGATE_PAIR);
    assertEscapeLonelySurrogates(SURROGATE_PAIR + "\\uDE00", SURROGATE_PAIR + LOW_SURROGATE);
    assertEscapeLonelySurrogates("\\uDE00a\\uD83D", LOW_SURROGATE + "a" + HIGH_SURROGATE);
  }

  /**
   * Asserts that {@link WholeProgramInferenceJavaParserStorage#indexOfLonelySurrogateCharacter}
   * returns {@code expected} for {@code s}.
   *
   * @param expected the expected index
   * @param s a string
   */
  private static void assertIndexOfLonelySurrogateCharacter(int expected, String s) {
    Assert.assertEquals(
        "indexOfLonelySurrogateCharacter(" + escapeAll(s) + ")",
        expected,
        WholeProgramInferenceJavaParserStorage.indexOfLonelySurrogateCharacter(s));
  }

  /**
   * Asserts that {@link WholeProgramInferenceJavaParserStorage#escapeLonelySurrogates} returns
   * {@code expected} for {@code s}.
   *
   * @param expected the expected result
   * @param s a string
   */
  private static void assertEscapeLonelySurrogates(String expected, String s) {
    Assert.assertEquals(
        "escapeLonelySurrogates(" + escapeAll(s) + ")",
        expected,
        WholeProgramInferenceJavaParserStorage.escapeLonelySurrogates(s));
  }

  /**
   * Returns the argument with every non-ASCII character replaced by its unicode escape, so that
   * assertion failure messages are readable.
   *
   * @param s a string
   * @return the string, with every non-ASCII character replaced by its unicode escape
   */
  private static String escapeAll(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < 128) {
        sb.append(c);
      } else {
        sb.append(String.format("\\u%04X", (int) c));
      }
    }
    return sb.toString();
  }
}
