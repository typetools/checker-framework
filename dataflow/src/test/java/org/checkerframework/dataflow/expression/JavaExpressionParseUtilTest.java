package org.checkerframework.dataflow.expression;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import org.junit.Assert;
import org.junit.Test;

public class JavaExpressionParseUtilTest {

  private final ProcessingEnvironment env;

  public JavaExpressionParseUtilTest() {
    Context context = new Context();

    // Set source and target to 8
    Options options = Options.instance(context);
    options.put(Option.SOURCE, "8");
    options.put(Option.TARGET, "8");

    env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // Even though source/target are set to 8, the modules in the JavaCompiler
    // need to be initialized by setting the list of modules to nil.
    javac.initModules(List.nil());
    javac.enterDone();
  }

  private JavaExpression parse(String expression) {
    try {
      return JavaExpressionParseUtil.parse(
          expression,
          null /* enclosingType */,
          null,
          null,
          null,
          null /* pathToCompilationUnit*/,
          env);
    } catch (JavaExpressionParseException e) {
      throw new Error(expression, e);
    }
  }

  // TODO: Add many tests.

  @Test
  public void m() {
    parse("1");
  }

  /** The negation of a floating-point literal is folded into a literal. */
  @Test
  public void negateFloatingPointLiteral() {
    JavaExpression negation = parse("-1.5");
    Assert.assertTrue(negation instanceof ValueLiteral);
    Assert.assertEquals(TypeKind.DOUBLE, negation.getType().getKind());
    Assert.assertEquals(-1.5, ((ValueLiteral) negation).getValue());

    negation = parse("-1.5f");
    Assert.assertTrue(negation instanceof ValueLiteral);
    Assert.assertEquals(TypeKind.FLOAT, negation.getType().getKind());
    Assert.assertEquals(-1.5f, ((ValueLiteral) negation).getValue());
  }

  /** The negation of a non-numeric literal is not folded. */
  @Test
  public void negateNonNumericLiteral() {
    Assert.assertTrue(parse("-true") instanceof UnaryOperation);
    Assert.assertTrue(parse("-\"s\"") instanceof UnaryOperation);
  }
}
