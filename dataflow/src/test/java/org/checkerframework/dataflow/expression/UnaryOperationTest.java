package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.junit.Assert;
import org.junit.Test;

/** Tests {@link UnaryOperation}. */
public class UnaryOperationTest {

  /** The type utilities. */
  private final Types types;

  /** Creates a UnaryOperationTest. */
  public UnaryOperationTest() {
    Context context = new Context();
    Options options = Options.instance(context);
    options.put(Option.SOURCE, "8");
    options.put(Option.TARGET, "8");
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    javac.initModules(List.nil());
    javac.enterDone();
    types = env.getTypeUtils();
  }

  /** The type of a unary operation is the given type, not the type of its operand. */
  @Test
  public void typeIsResultType() {
    TypeMirror byteType = types.getPrimitiveType(TypeKind.BYTE);
    TypeMirror intType = types.getPrimitiveType(TypeKind.INT);
    // Unary numeric promotion:  the negation of a byte is an int.
    UnaryOperation negation =
        new UnaryOperation(intType, Tree.Kind.UNARY_MINUS, new ValueLiteral(byteType, (byte) 5));
    Assert.assertEquals(TypeKind.INT, negation.getType().getKind());
  }
}
