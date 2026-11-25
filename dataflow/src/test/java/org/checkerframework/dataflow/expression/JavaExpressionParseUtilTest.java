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

  /** A parsed unary operation has the type that unary numeric promotion gives it. */
  @Test
  public void unaryOperationPromotedType() {
    // Unary numeric promotion:  the bitwise complement of a char is an int.
    JavaExpression complement = parse("~'a'");
    Assert.assertTrue(complement instanceof UnaryOperation);
    Assert.assertEquals(TypeKind.INT, complement.getType().getKind());

    // Unary numeric promotion:  the unary plus of a char is an int.
    JavaExpression plus = parse("+'a'");
    Assert.assertTrue(plus instanceof UnaryOperation);
    Assert.assertEquals(TypeKind.INT, plus.getType().getKind());

    // A unary plus that does not change the type is eliminated.
    JavaExpression plusInt = parse("+1");
    Assert.assertTrue(plusInt instanceof ValueLiteral);
    Assert.assertEquals(TypeKind.INT, plusInt.getType().getKind());
  }

  /** A parsed binary operation has the type that binary numeric promotion gives it. */
  @Test
  public void binaryOperationPromotedType() {
    // Binary numeric promotion:  the sum of two chars is an int.
    Assert.assertEquals(TypeKind.INT, parse("'a' + 'b'").getType().getKind());
    Assert.assertEquals(TypeKind.LONG, parse("1 + 2L").getType().getKind());
    Assert.assertEquals(TypeKind.DOUBLE, parse("1L * 2.0").getType().getKind());
    Assert.assertEquals(TypeKind.BOOLEAN, parse("'a' < 2.0").getType().getKind());
    // The type of a shift is the promoted type of its left operand.
    Assert.assertEquals(TypeKind.INT, parse("1 << 2L").getType().getKind());
    Assert.assertEquals(TypeKind.INT, parse("'a' >> 1").getType().getKind());
    Assert.assertEquals(TypeKind.LONG, parse("1L >>> 2").getType().getKind());
  }
}
