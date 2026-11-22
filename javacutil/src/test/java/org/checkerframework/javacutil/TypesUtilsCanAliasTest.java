package org.checkerframework.javacutil;

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link TypesUtils#canAlias}. */
public class TypesUtilsCanAliasTest {

  /** Creates a new TypesUtilsCanAliasTest. */
  public TypesUtilsCanAliasTest() {}

  /** The processing environment. All the tests share one, because setting one up is slow. */
  private static final ProcessingEnvironment env;

  /** The javac type utilities, which create intersection types and least upper bounds. */
  private static final com.sun.tools.javac.code.Types javacTypes;

  /** The type utilities. */
  private static final Types types;

  /** The javac name table, which creates the name of a type variable. */
  private static final Names names;

  /** The javac symbol table, which supplies the owner and lower bound of a type variable. */
  private static final Symtab symtab;

  static {
    Context context = new Context();
    env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();
    javacTypes = com.sun.tools.javac.code.Types.instance(context);
    types = env.getTypeUtils();
    names = Names.instance(context);
    symtab = Symtab.instance(context);
  }

  /**
   * Returns the type of the given class or interface, such as {@code java.lang.Integer}.
   *
   * @param canonicalName the canonical name of a class or interface
   * @return the type of the given class or interface
   */
  private static Type type(String canonicalName) {
    return (Type) env.getElementUtils().getTypeElement(canonicalName).asType();
  }

  /**
   * Returns the intersection of the given types.
   *
   * @param first a type, which becomes the first bound of the intersection type
   * @param second another type, which becomes the second bound of the intersection type
   * @return the intersection type {@code first & second}
   */
  private static Type intersection(Type first, Type second) {
    return javacTypes.makeIntersectionType(com.sun.tools.javac.util.List.of(first, second));
  }

  /**
   * Returns the union of the given types, which is the type of a multi-catch exception parameter.
   *
   * @param first an exception type, which becomes the first alternative of the union type
   * @param second another exception type, which becomes the second alternative of the union type
   * @return the union type {@code first | second}
   */
  private static TypeMirror union(Type first, Type second) {
    ClassType lub = (ClassType) javacTypes.lub(first, second);
    return new UnionClassType(lub, com.sun.tools.javac.util.List.of(first, second));
  }

  /**
   * Asserts that {@link TypesUtils#canAlias} returns the expected result for the given types, in
   * either order.
   *
   * @param expected the expected result
   * @param a a type
   * @param b another type
   */
  private static void assertCanAlias(boolean expected, TypeMirror a, TypeMirror b) {
    Assert.assertEquals(a + " and " + b, expected, TypesUtils.canAlias(a, b, types));
    Assert.assertEquals(b + " and " + a, expected, TypesUtils.canAlias(b, a, types));
  }

  /** Declared types can alias exactly when one is a subtype of the other. */
  @Test
  public void declaredTypes() {
    assertCanAlias(true, type("java.lang.Integer"), type("java.lang.Number"));
    assertCanAlias(false, type("java.lang.Integer"), type("java.lang.String"));
  }

  /**
   * A union type can alias a type that is related to one of its alternatives, but not a type that
   * is unrelated to every alternative, even if that type is related to the alternatives' least
   * upper bound.
   */
  @Test
  public void unionType() {
    TypeMirror integerOrLong = union(type("java.lang.Integer"), type("java.lang.Long"));
    assertCanAlias(true, integerOrLong, type("java.lang.Integer"));
    assertCanAlias(true, integerOrLong, type("java.lang.Number"));
    assertCanAlias(true, integerOrLong, type("java.lang.Object"));
    // The erasure of the union type is Number, which is unrelated to Comparable, but Integer and
    // Long are both Comparable.
    assertCanAlias(true, integerOrLong, type("java.lang.Comparable"));
    // The least upper bound of Integer and Long is Number & Comparable<...> & ..., whose erasure
    // is Number, which is a supertype of Double.
    assertCanAlias(false, integerOrLong, type("java.lang.Double"));
  }

  /**
   * Returns a type variable {@code T extends Number & Runnable}. The erasure of {@code T} is {@code
   * Number}, which is unrelated to {@code Runnable}.
   *
   * @return a type variable whose upper bound is {@code Number & Runnable}
   */
  private static TypeVar numberAndRunnableTypeVariable() {
    TypeVar result = new TypeVar(names.fromString("T"), symtab.noSymbol, symtab.botType);
    result.setUpperBound(intersection(type("java.lang.Number"), type("java.lang.Runnable")));
    return result;
  }

  /** A type variable can alias a type that is related to any of its bounds. */
  @Test
  public void typeVariableWithIntersectionBound() {
    TypeMirror typeVariable = numberAndRunnableTypeVariable();
    assertCanAlias(true, typeVariable, type("java.lang.Runnable"));
    assertCanAlias(true, typeVariable, type("java.lang.Integer"));
    assertCanAlias(false, typeVariable, type("java.lang.String"));
  }

  /**
   * A wildcard whose extends bound is a type variable can alias a type that is related to any bound
   * of the type variable.
   */
  @Test
  public void wildcardExtendingTypeVariableWithIntersectionBound() {
    TypeMirror wildcard = types.getWildcardType(numberAndRunnableTypeVariable(), null);
    assertCanAlias(true, wildcard, type("java.lang.Runnable"));
    assertCanAlias(true, wildcard, type("java.lang.Integer"));
    assertCanAlias(false, wildcard, type("java.lang.String"));
  }

  /**
   * Returns a wildcard that is a type argument for a type parameter {@code T extends Number &
   * Runnable}, as the {@code ?} in {@code Box<?>} is if {@code Box} is declared as {@code class
   * Box<T extends Number & Runnable>}.
   *
   * @param kind a kind of wildcard
   * @param bound the extends bound or super bound of the wildcard, or {@code Object} if {@code
   *     kind} is {@link BoundKind#UNBOUND}
   * @return a wildcard whose type parameter's upper bound is {@code Number & Runnable}
   */
  private static TypeMirror wildcardForNumberAndRunnableTypeParameter(BoundKind kind, Type bound) {
    return new Type.WildcardType(bound, kind, symtab.boundClass, numberAndRunnableTypeVariable());
  }

  /**
   * A wildcard that has no extends bound can alias a type that is related to any bound of the type
   * parameter that the wildcard is a type argument for.
   */
  @Test
  public void wildcardWithoutExtendsBoundForTypeParameterWithIntersectionBound() {
    TypeMirror unbounded =
        wildcardForNumberAndRunnableTypeParameter(BoundKind.UNBOUND, symtab.objectType);
    TypeMirror superInteger =
        wildcardForNumberAndRunnableTypeParameter(BoundKind.SUPER, type("java.lang.Integer"));
    for (TypeMirror wildcard : new TypeMirror[] {unbounded, superInteger}) {
      assertCanAlias(true, wildcard, type("java.lang.Runnable"));
      assertCanAlias(true, wildcard, type("java.lang.Integer"));
      assertCanAlias(false, wildcard, type("java.lang.String"));
    }
  }

  /**
   * A wildcard that has an extends bound can alias a type that is related to the extends bound or
   * to any bound of the type parameter that the wildcard is a type argument for.
   */
  @Test
  public void wildcardWithExtendsBoundForTypeParameterWithIntersectionBound() {
    TypeMirror wildcard =
        wildcardForNumberAndRunnableTypeParameter(
            BoundKind.EXTENDS, type("java.lang.CharSequence"));
    assertCanAlias(true, wildcard, type("java.lang.CharSequence"));
    assertCanAlias(true, wildcard, type("java.lang.Runnable"));
    assertCanAlias(true, wildcard, type("java.lang.Integer"));
    assertCanAlias(false, wildcard, type("java.lang.Boolean"));
  }

  /** A wildcard that has no upper bound can alias any reference type. */
  @Test
  public void unboundedWildcards() {
    TypeMirror unbounded = types.getWildcardType(null, null);
    TypeMirror superInteger = types.getWildcardType(null, type("java.lang.Integer"));
    assertCanAlias(true, unbounded, type("java.lang.String"));
    assertCanAlias(true, superInteger, type("java.lang.String"));
  }

  /** An array of a type variable can alias an array of any of the type variable's bounds. */
  @Test
  public void arrayOfTypeVariableWithIntersectionBound() {
    TypeMirror array = types.getArrayType(numberAndRunnableTypeVariable());
    assertCanAlias(true, array, types.getArrayType(type("java.lang.Runnable")));
    assertCanAlias(true, array, types.getArrayType(type("java.lang.Integer")));
    assertCanAlias(false, array, types.getArrayType(type("java.lang.String")));
  }

  /**
   * Asserts that {@link TypesUtils#canAliasConservative} returns the expected result for the given
   * types, in either order.
   *
   * @param expected the expected result
   * @param a a type
   * @param b another type
   */
  private static void assertCanAliasConservative(boolean expected, TypeMirror a, TypeMirror b) {
    Assert.assertEquals(a + " and " + b, expected, TypesUtils.canAliasConservative(a, b, types));
    Assert.assertEquals(b + " and " + a, expected, TypesUtils.canAliasConservative(b, a, types));
  }

  /**
   * The conservative check treats an interface as possibly aliasing any class that is not final,
   * and arrays of such types as possibly aliasing.
   */
  @Test
  public void conservativeInterfaces() {
    Type number = type("java.lang.Number");
    Type comparable = (Type) types.erasure(type("java.lang.Comparable"));
    Type runnable = type("java.lang.Runnable");
    Type string = type("java.lang.String");
    Type integer = type("java.lang.Integer");
    assertCanAlias(false, number, comparable);
    assertCanAliasConservative(true, number, comparable);
    assertCanAliasConservative(true, runnable, comparable);
    assertCanAliasConservative(true, types.getArrayType(number), types.getArrayType(comparable));
    assertCanAliasConservative(
        true,
        types.getArrayType(types.getArrayType(number)),
        types.getArrayType(types.getArrayType(runnable)));
    // String is final and does not implement Runnable.
    assertCanAliasConservative(false, string, runnable);
    assertCanAliasConservative(false, types.getArrayType(string), types.getArrayType(runnable));
    // Two unrelated classes.
    assertCanAliasConservative(false, integer, string);
    assertCanAliasConservative(false, types.getArrayType(integer), types.getArrayType(string));
    assertCanAliasConservative(
        false, types.getArrayType(number), types.getArrayType(types.getArrayType(number)));
  }

  /** Arrays with different primitive component types never alias, even if one widens to the other. */
  @Test
  public void conservativePrimitiveArrays() {
    TypeMirror intType = types.getPrimitiveType(TypeKind.INT);
    TypeMirror longType = types.getPrimitiveType(TypeKind.LONG);
    TypeMirror intArray = types.getArrayType(intType);
    TypeMirror longArray = types.getArrayType(longType);
    assertCanAliasConservative(true, intArray, types.getArrayType(intType));
    assertCanAliasConservative(false, intArray, longArray);
    assertCanAliasConservative(false, types.getArrayType(intArray), types.getArrayType(longArray));
    assertCanAliasConservative(false, intArray, types.getArrayType(type("java.lang.Integer")));
  }
}
