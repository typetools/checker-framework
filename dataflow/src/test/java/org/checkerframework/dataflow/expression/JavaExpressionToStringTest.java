package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link JavaExpression#toString} parenthesizes an operand of a postfix operation, such
 * as the receiver of a method call or the array of an array access, when necessary, and that it
 * prints a class name without type arguments.
 */
public class JavaExpressionToStringTest {

  /** The type utilities. */
  private final Types types;

  /** The element utilities. */
  private final Elements elements;

  /** The type {@code String}. */
  private final TypeMirror stringType;

  /** The type {@code Integer}. */
  private final TypeMirror integerType;

  /** The type {@code int}. */
  private final TypeMirror intType;

  /** The method {@code String.trim()}. */
  private final ExecutableElement trimMethod;

  /** The method {@code Integer.intValue()}. */
  private final ExecutableElement intValueMethod;

  /** The instance field {@code Integer.value}. */
  private final VariableElement valueField;

  /** Creates a JavaExpressionToStringTest. */
  public JavaExpressionToStringTest() {
    Context context = new Context();
    Options options = Options.instance(context);
    options.put(Option.SOURCE, "8");
    options.put(Option.TARGET, "8");
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    types = env.getTypeUtils();
    elements = env.getElementUtils();
    TypeElement stringElement = elements.getTypeElement("java.lang.String");
    TypeElement integerElement = elements.getTypeElement("java.lang.Integer");
    stringType = stringElement.asType();
    integerType = integerElement.asType();
    intType = types.getPrimitiveType(TypeKind.INT);
    trimMethod = getMethod(stringElement, "trim");
    intValueMethod = getMethod(integerElement, "intValue");
    valueField = getField(integerElement, "value");
  }

  /**
   * Returns the method with the given name and no parameters, declared in the given type.
   *
   * @param type a type
   * @param name the name of a method in the type
   * @return the method with the given name and no parameters, declared in the type
   */
  private static ExecutableElement getMethod(TypeElement type, String name) {
    for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
      if (method.getSimpleName().contentEquals(name) && method.getParameters().isEmpty()) {
        return method;
      }
    }
    throw new Error("No method " + name + "() in " + type);
  }

  /**
   * Returns the field with the given name, declared in the given type.
   *
   * @param type a type
   * @param name the name of a field in the type
   * @return the field with the given name, declared in the type
   */
  private static VariableElement getField(TypeElement type, String name) {
    for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
      if (field.getSimpleName().contentEquals(name)) {
        return field;
      }
    }
    throw new Error("No field " + name + " in " + type);
  }

  /**
   * Returns the call to {@code String.trim()} on the given receiver.
   *
   * @param receiver a receiver of type String
   * @return the call to {@code trim()} on the receiver
   */
  private MethodCall trim(JavaExpression receiver) {
    return new MethodCall(stringType, trimMethod, receiver, Collections.emptyList());
  }

  /**
   * Returns the call to {@code Integer.intValue()} on the given receiver.
   *
   * @param receiver a receiver of type Integer
   * @return the call to {@code intValue()} on the receiver
   */
  private MethodCall intValue(JavaExpression receiver) {
    return new MethodCall(intType, intValueMethod, receiver, Collections.emptyList());
  }

  /**
   * Returns the int literal for the given value.
   *
   * @param value an int
   * @return the int literal for the value
   */
  private ValueLiteral intLiteral(int value) {
    return new ValueLiteral(intType, value);
  }

  @Test
  public void methodCallOnAtomicReceiver() {
    ValueLiteral abc = new ValueLiteral(stringType, "abc");
    Assert.assertEquals("\"abc\".trim()", trim(abc).toString());
    Assert.assertEquals("\"abc\".trim().trim()", trim(trim(abc)).toString());
  }

  @Test
  public void methodCallOnBinaryOperation() {
    BinaryOperation concatenation =
        new BinaryOperation(
            stringType,
            Tree.Kind.PLUS,
            new ValueLiteral(stringType, "a"),
            new ValueLiteral(stringType, "b"));
    Assert.assertEquals("(\"a\" + \"b\").trim()", trim(concatenation).toString());
  }

  @Test
  public void methodCallOnUnaryOperation() {
    UnaryOperation negation = new UnaryOperation(integerType, Tree.Kind.UNARY_MINUS, intLiteral(5));
    Assert.assertEquals("(-5).intValue()", intValue(negation).toString());
  }

  @Test
  public void methodCallOnNumericLiteral() {
    Assert.assertEquals("(5).intValue()", intValue(intLiteral(5)).toString());
    Assert.assertEquals("(-1).intValue()", intValue(intLiteral(-1)).toString());
  }

  @Test
  public void methodCallOnUnknown() {
    Assert.assertEquals(
        "((String) o).trim()", trim(new Unknown(stringType, "(String) o")).toString());
    Assert.assertEquals(
        "(b ? x : y).trim()", trim(new Unknown(stringType, "b ? x : y")).toString());
  }

  @Test
  public void fieldAccessOnUnknown() {
    FieldAccess access = new FieldAccess(new Unknown(integerType, "(Integer) o"), valueField);
    Assert.assertEquals("((Integer) o).value", access.toString());
  }

  @Test
  public void arrayAccessOnArrayCreation() {
    TypeMirror intArrayType = types.getArrayType(intType);
    TypeMirror intArrayArrayType = types.getArrayType(intArrayType);
    List<@Nullable JavaExpression> dimensions = List.of(intLiteral(3), intLiteral(4));
    ArrayCreation creation =
        new ArrayCreation(intArrayArrayType, dimensions, Collections.emptyList());
    Assert.assertEquals("new int[3][4]", creation.toString());
    ArrayAccess access = new ArrayAccess(intArrayType, creation, intLiteral(0));
    Assert.assertEquals("(new int[3][4])[0]", access.toString());
  }

  @Test
  public void staticFieldAccessOfGenericClass() {
    TypeElement arrayListElement = elements.getTypeElement("java.util.ArrayList");
    // The receiver's type is `java.util.ArrayList<E>`.
    FieldAccess access =
        new FieldAccess(
            new ClassName(arrayListElement.asType()),
            getField(arrayListElement, "DEFAULT_CAPACITY"));
    Assert.assertEquals("java.util.ArrayList.DEFAULT_CAPACITY", access.toString());
  }

  @Test
  public void staticMethodCallOfGenericClass() {
    TypeElement optionalElement = elements.getTypeElement("java.util.Optional");
    // The receiver's type is `java.util.Optional<T>`.
    MethodCall call =
        new MethodCall(
            optionalElement.asType(),
            getMethod(optionalElement, "empty"),
            new ClassName(optionalElement.asType()),
            Collections.emptyList());
    Assert.assertEquals("java.util.Optional.empty()", call.toString());
  }

  @Test
  public void classNameOfInnerClassOfGenericClass() {
    TypeElement keySetElement = elements.getTypeElement("java.util.HashMap.KeySet");
    // The type is `java.util.HashMap<K,V>.KeySet`.
    ClassName className = new ClassName(keySetElement.asType());
    Assert.assertEquals("java.util.HashMap.KeySet", className.getTypeString());
    Assert.assertEquals("java.util.HashMap.KeySet.class", className.toString());
  }
}
