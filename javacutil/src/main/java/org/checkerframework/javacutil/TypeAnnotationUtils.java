package org.checkerframework.javacutil;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A collection of helper methods related to type annotation handling.
 *
 * @see AnnotationUtils
 */
public class TypeAnnotationUtils {

  // Class cannot be instantiated.
  private TypeAnnotationUtils() {
    throw new AssertionError("Class TypeAnnotationUtils cannot be instantiated.");
  }

  /**
   * Check whether a TypeCompound is contained in a list of TypeCompounds.
   *
   * @param list the input list of TypeCompounds
   * @param tc the TypeCompound to find
   * @param types type utilities
   * @return true, iff a TypeCompound equal to tc is contained in list
   */
  public static boolean isTypeCompoundContained(
      List<TypeCompound> list, TypeCompound tc, Types types) {
    for (Attribute.TypeCompound rawat : list) {
      if (typeCompoundEquals(rawat, tc, types)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compares two TypeCompound objects (e.g., annotations).
   *
   * @param tc1 the first TypeCompound to compare
   * @param tc2 the second TypeCompound to compare
   * @param types type utilities
   * @return true if the TypeCompounds represent the same compound element value
   */
  private static boolean typeCompoundEquals(TypeCompound tc1, TypeCompound tc2, Types types) {
    // For the first conjunct, both of these forms fail in some cases:
    //   tc1.type == tc2.type
    //   types.isSameType(tc1.type, tc2.type)
    return contentEquals(tc1.type.tsym.name, tc2.type.tsym.name)
        && typeCompoundValuesEquals(tc1.values, tc2.values, types)
        && isSameTAPositionExceptTreePos(tc1.position, tc2.position);
  }

  /**
   * Returns true if the two names represent the same string.
   *
   * @param n1 the first Name to compare
   * @param n2 the second Name to compare
   * @return true if the two names represent the same string
   */
  @SuppressWarnings(
      "interning:unnecessary.equals" // Name is interned within a single instance of javac,
  // but call equals anyway out of paranoia.
  )
  private static boolean contentEquals(Name n1, Name n2) {
    if (n1.getClass() == n2.getClass()) {
      return n1.equals(n2);
    } else {
      // Slightly less efficient because it makes a copy.
      return n1.contentEquals(n2);
    }
  }

  /**
   * Compares the {@code values} fields of two TypeCompound objects (e.g., annotations). Is more
   * lenient than {@code List.equals}, which uses {@code Object.equals} on list elements.
   *
   * @param values1 the first {@code values} field
   * @param values2 the second {@code values} field
   * @param types type utilities
   * @return true if the two {@code values} fields represent the same name-to-value mapping, in the
   *     same order
   */
  @SuppressWarnings("InvalidParam") // Error Prone tries to be clever, but it is not
  private static boolean typeCompoundValuesEquals(
      List<Pair<MethodSymbol, Attribute>> values1,
      List<Pair<MethodSymbol, Attribute>> values2,
      Types types) {
    if (values1.size() != values2.size()) {
      return false;
    }

    for (Iterator<Pair<MethodSymbol, Attribute>> iter1 = values1.iterator(),
            iter2 = values2.iterator();
        iter1.hasNext(); ) {
      Pair<MethodSymbol, Attribute> pair1 = iter1.next();
      Pair<MethodSymbol, Attribute> pair2 = iter2.next();
      if (!(pair1.fst.equals(pair2.fst) && attributeEquals(pair1.snd, pair2.snd, types))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares two attributes. Is more lenient for constants than {@code Attribute.equals}, which is
   * reference equality.
   *
   * @param a1 the first attribute to compare
   * @param a2 the second attribute to compare
   * @param types type utilities
   * @return true if the two attributes are the same
   */
  private static boolean attributeEquals(Attribute a1, Attribute a2, Types types) {
    if (a1 instanceof Attribute.Array && a2 instanceof Attribute.Array) {
      List<Attribute> list1 = ((Attribute.Array) a1).getValue();
      List<Attribute> list2 = ((Attribute.Array) a2).getValue();
      if (list1.size() != list2.size()) {
        return false;
      }
      // This requires the array elements to be in the same order.  Is that the right thing?
      for (int i = 0; i < list1.size(); i++) {
        if (!attributeEquals(list1.get(i), list2.get(i), types)) {
          return false;
        }
      }
      return true;
    } else if (a1 instanceof Attribute.Class && a2 instanceof Attribute.Class) {
      Type t1 = ((Attribute.Class) a1).getValue();
      Type t2 = ((Attribute.Class) a2).getValue();
      return types.isSameType(t1, t2);
    } else if (a1 instanceof Attribute.Constant && a2 instanceof Attribute.Constant) {
      Object v1 = ((Attribute.Constant) a1).getValue();
      Object v2 = ((Attribute.Constant) a2).getValue();
      return v1.equals(v2);
    } else if (a1 instanceof Attribute.Compound && a2 instanceof Attribute.Compound) {
      // The annotation value is another annotation.  `a1` and `a2` implement AnnotationMirror.
      DeclaredType t1 = ((Attribute.Compound) a1).getAnnotationType();
      DeclaredType t2 = ((Attribute.Compound) a2).getAnnotationType();
      if (!types.isSameType(t1, t2)) {
        return false;
      }
      Map<Symbol.MethodSymbol, Attribute> map1 = ((Attribute.Compound) a1).getElementValues();
      Map<Symbol.MethodSymbol, Attribute> map2 = ((Attribute.Compound) a2).getElementValues();
      // Is this test, which uses equals() for the keys, too strict?
      if (!map1.keySet().equals(map2.keySet())) {
        return false;
      }
      for (Symbol.MethodSymbol key : map1.keySet()) {
        Attribute attr1 = map1.get(key);
        @SuppressWarnings("nullness:assignment") // same keys in map1 & map2
        @NonNull Attribute attr2 = map2.get(key);
        if (!attributeEquals(attr1, attr2, types)) {
          return false;
        }
      }
      return true;
    } else if (a1 instanceof Attribute.Enum && a2 instanceof Attribute.Enum) {
      Symbol.VarSymbol s1 = ((Attribute.Enum) a1).getValue();
      Symbol.VarSymbol s2 = ((Attribute.Enum) a2).getValue();
      // VarSymbol.equals() is reference equality.
      return s1.equals(s2) || s1.toString().equals(s2.toString());
    } else if (a1 instanceof Attribute.Error && a2 instanceof Attribute.Error) {
      String s1 = ((Attribute.Error) a1).getValue();
      String s2 = ((Attribute.Error) a2).getValue();
      return s1.equals(s2);
    } else {
      return a1.equals(a2);
    }
  }

  /**
   * Compare two TypeAnnotationPositions for equality.
   *
   * @param p1 the first position
   * @param p2 the second position
   * @return true, iff the two positions are equal
   */
  public static boolean isSameTAPosition(TypeAnnotationPosition p1, TypeAnnotationPosition p2) {
    return isSameTAPositionExceptTreePos(p1, p2) && p1.pos == p2.pos;
  }

  /**
   * Compare two TypeAnnotationPositions for equality, ignoring the source tree position.
   *
   * @param p1 the first position
   * @param p2 the second position
   * @return true, iff the two positions are equal except for the source tree position
   */
  @SuppressWarnings("interning:not.interned") // reference equality for onLambda field
  public static boolean isSameTAPositionExceptTreePos(
      TypeAnnotationPosition p1, TypeAnnotationPosition p2) {
    return p1.type == p2.type
        && p1.type_index == p2.type_index
        && p1.bound_index == p2.bound_index
        && p1.onLambda == p2.onLambda
        && p1.parameter_index == p2.parameter_index
        && p1.isValidOffset == p2.isValidOffset
        && p1.offset == p2.offset
        && p1.location.equals(p2.location)
        && Arrays.equals(p1.lvarIndex, p2.lvarIndex)
        && Arrays.equals(p1.lvarLength, p2.lvarLength)
        && Arrays.equals(p1.lvarOffset, p2.lvarOffset)
        && (!p1.hasExceptionIndex()
            || !p2.hasExceptionIndex()
            || (p1.getExceptionIndex() == p2.getExceptionIndex()));
  }

  /**
   * Returns a newly created Attribute.Compound corresponding to an argument AnnotationMirror.
   *
   * @param am an AnnotationMirror, which may be part of an AST or an internally created subclass
   * @return a new Attribute.Compound corresponding to the AnnotationMirror
   */
  public static Attribute.Compound createCompoundFromAnnotationMirror(
      AnnotationMirror am, ProcessingEnvironment env) {
    // Create a new Attribute to match the AnnotationMirror.
    List<Pair<Symbol.MethodSymbol, Attribute>> values = List.nil();
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        am.getElementValues().entrySet()) {
      Attribute attribute = attributeFromAnnotationValue(entry.getKey(), entry.getValue(), env);
      values = values.append(new Pair<>((Symbol.MethodSymbol) entry.getKey(), attribute));
    }
    return new Attribute.Compound((Type.ClassType) am.getAnnotationType(), values);
  }

  /**
   * Returns a newly created Attribute.TypeCompound corresponding to an argument AnnotationMirror.
   *
   * @param am an AnnotationMirror, which may be part of an AST or an internally created subclass
   * @param tapos the type annotation position to use
   * @return a new Attribute.TypeCompound corresponding to the AnnotationMirror
   */
  public static Attribute.TypeCompound createTypeCompoundFromAnnotationMirror(
      AnnotationMirror am, TypeAnnotationPosition tapos, ProcessingEnvironment env) {
    // Create a new Attribute to match the AnnotationMirror.
    List<Pair<Symbol.MethodSymbol, Attribute>> values = List.nil();
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        am.getElementValues().entrySet()) {
      Attribute attribute = attributeFromAnnotationValue(entry.getKey(), entry.getValue(), env);
      values = values.append(new Pair<>((Symbol.MethodSymbol) entry.getKey(), attribute));
    }
    return new Attribute.TypeCompound((Type.ClassType) am.getAnnotationType(), values, tapos);
  }

  /**
   * Returns a newly created Attribute corresponding to an argument AnnotationValue.
   *
   * @param meth the ExecutableElement that is assigned the value, needed for empty arrays
   * @param av an AnnotationValue, which may be part of an AST or an internally created subclass
   * @return a new Attribute corresponding to the AnnotationValue
   */
  public static Attribute attributeFromAnnotationValue(
      ExecutableElement meth, AnnotationValue av, ProcessingEnvironment env) {
    return av.accept(new AttributeCreator(env, meth), null);
  }

  private static class AttributeCreator implements AnnotationValueVisitor<Attribute, Void> {
    private final ProcessingEnvironment processingEnv;
    private final Types modelTypes;
    private final Elements elements;
    private final com.sun.tools.javac.code.Types javacTypes;

    private final ExecutableElement meth;

    public AttributeCreator(ProcessingEnvironment env, ExecutableElement meth) {
      this.processingEnv = env;
      Context context = ((JavacProcessingEnvironment) env).getContext();
      this.elements = env.getElementUtils();
      this.modelTypes = env.getTypeUtils();
      this.javacTypes = com.sun.tools.javac.code.Types.instance(context);

      this.meth = meth;
    }

    @Override
    public Attribute visit(AnnotationValue av, Void p) {
      return av.accept(this, p);
    }

    @Override
    public Attribute visit(AnnotationValue av) {
      return visit(av, null);
    }

    @Override
    public Attribute visitBoolean(boolean b, Void p) {
      TypeMirror booleanType = modelTypes.getPrimitiveType(TypeKind.BOOLEAN);
      return new Attribute.Constant((Type) booleanType, b ? 1 : 0);
    }

    @Override
    public Attribute visitByte(byte b, Void p) {
      TypeMirror byteType = modelTypes.getPrimitiveType(TypeKind.BYTE);
      return new Attribute.Constant((Type) byteType, b);
    }

    @Override
    public Attribute visitChar(char c, Void p) {
      TypeMirror charType = modelTypes.getPrimitiveType(TypeKind.CHAR);
      return new Attribute.Constant((Type) charType, c);
    }

    @Override
    public Attribute visitDouble(double d, Void p) {
      TypeMirror doubleType = modelTypes.getPrimitiveType(TypeKind.DOUBLE);
      return new Attribute.Constant((Type) doubleType, d);
    }

    @Override
    public Attribute visitFloat(float f, Void p) {
      TypeMirror floatType = modelTypes.getPrimitiveType(TypeKind.FLOAT);
      return new Attribute.Constant((Type) floatType, f);
    }

    @Override
    public Attribute visitInt(int i, Void p) {
      TypeMirror intType = modelTypes.getPrimitiveType(TypeKind.INT);
      return new Attribute.Constant((Type) intType, i);
    }

    @Override
    public Attribute visitLong(long i, Void p) {
      TypeMirror longType = modelTypes.getPrimitiveType(TypeKind.LONG);
      return new Attribute.Constant((Type) longType, i);
    }

    @Override
    public Attribute visitShort(short s, Void p) {
      TypeMirror shortType = modelTypes.getPrimitiveType(TypeKind.SHORT);
      return new Attribute.Constant((Type) shortType, s);
    }

    @Override
    public Attribute visitString(String s, Void p) {
      TypeMirror stringType = elements.getTypeElement("java.lang.String").asType();
      return new Attribute.Constant((Type) stringType, s);
    }

    @Override
    public Attribute visitType(TypeMirror t, Void p) {
      if (t instanceof Type) {
        return new Attribute.Class(javacTypes, (Type) t);
      } else {
        throw new BugInCF("Unexpected type of TypeMirror: " + t.getClass());
      }
    }

    @Override
    public Attribute visitEnumConstant(VariableElement c, Void p) {
      if (c instanceof Symbol.VarSymbol) {
        Symbol.VarSymbol sym = (Symbol.VarSymbol) c;
        if (sym.getKind() == ElementKind.ENUM_CONSTANT) {
          return new Attribute.Enum(sym.type, sym);
        }
      }
      throw new BugInCF("Unexpected type of VariableElement: " + c.getClass());
    }

    @Override
    public Attribute visitAnnotation(AnnotationMirror a, Void p) {
      return createCompoundFromAnnotationMirror(a, processingEnv);
    }

    @Override
    public Attribute visitArray(java.util.List<? extends AnnotationValue> vals, Void p) {
      if (!vals.isEmpty()) {
        List<Attribute> valAttrs = List.nil();
        for (AnnotationValue av : vals) {
          valAttrs = valAttrs.append(av.accept(this, p));
        }
        ArrayType arrayType = modelTypes.getArrayType(valAttrs.get(0).type);
        return new Attribute.Array((Type) arrayType, valAttrs);
      } else {
        return new Attribute.Array((Type) meth.getReturnType(), List.nil());
      }
    }

    @Override
    public Attribute visitUnknown(AnnotationValue av, Void p) {
      throw new BugInCF("Unexpected type of AnnotationValue: " + av.getClass());
    }
  }

  /**
   * Create an unknown TypeAnnotationPosition.
   *
   * @return an unkown TypeAnnotationPosition
   */
  public static TypeAnnotationPosition unknownTAPosition() {
    return TypeAnnotationPosition.unknown;
  }

  /**
   * Create a method return TypeAnnotationPosition.
   *
   * @param pos the source tree position
   * @return a method return TypeAnnotationPosition
   */
  public static TypeAnnotationPosition methodReturnTAPosition(final int pos) {
    return TypeAnnotationPosition.methodReturn(pos);
  }

  /**
   * Create a method receiver TypeAnnotationPosition.
   *
   * @param pos the source tree position
   * @return a method receiver TypeAnnotationPosition
   */
  public static TypeAnnotationPosition methodReceiverTAPosition(final int pos) {
    return TypeAnnotationPosition.methodReceiver(pos);
  }

  /**
   * Create a method parameter TypeAnnotationPosition.
   *
   * @param pidx the parameter index
   * @param pos the source tree position
   * @return a method parameter TypeAnnotationPosition
   */
  public static TypeAnnotationPosition methodParameterTAPosition(final int pidx, final int pos) {
    return TypeAnnotationPosition.methodParameter(pidx, pos);
  }

  /**
   * Create a method throws TypeAnnotationPosition.
   *
   * @param tidx the throws index
   * @param pos the source tree position
   * @return a method throws TypeAnnotationPosition
   */
  public static TypeAnnotationPosition methodThrowsTAPosition(final int tidx, final int pos) {
    return TypeAnnotationPosition.methodThrows(TypeAnnotationPosition.emptyPath, null, tidx, pos);
  }

  /**
   * Create a field TypeAnnotationPosition.
   *
   * @param pos the source tree position
   * @return a field TypeAnnotationPosition
   */
  public static TypeAnnotationPosition fieldTAPosition(final int pos) {
    return TypeAnnotationPosition.field(pos);
  }

  /**
   * Create a class extends TypeAnnotationPosition.
   *
   * @param implidx the class extends index
   * @param pos the source tree position
   * @return a class extends TypeAnnotationPosition
   */
  public static TypeAnnotationPosition classExtendsTAPosition(final int implidx, final int pos) {
    return TypeAnnotationPosition.classExtends(implidx, pos);
  }

  /**
   * Create a type parameter TypeAnnotationPosition.
   *
   * @param tpidx the type parameter index
   * @param pos the source tree position
   * @return a type parameter TypeAnnotationPosition
   */
  public static TypeAnnotationPosition typeParameterTAPosition(final int tpidx, final int pos) {
    return TypeAnnotationPosition.typeParameter(TypeAnnotationPosition.emptyPath, null, tpidx, pos);
  }

  /**
   * Create a method type parameter TypeAnnotationPosition.
   *
   * @param tpidx the method type parameter index
   * @param pos the source tree position
   * @return a method type parameter TypeAnnotationPosition
   */
  public static TypeAnnotationPosition methodTypeParameterTAPosition(
      final int tpidx, final int pos) {
    return TypeAnnotationPosition.methodTypeParameter(
        TypeAnnotationPosition.emptyPath, null, tpidx, pos);
  }

  /**
   * Create a type parameter bound TypeAnnotationPosition.
   *
   * @param tpidx the type parameter index
   * @param bndidx the bound index
   * @param pos the source tree position
   * @return a method parameter TypeAnnotationPosition
   */
  public static TypeAnnotationPosition typeParameterBoundTAPosition(
      final int tpidx, final int bndidx, final int pos) {
    return TypeAnnotationPosition.typeParameterBound(
        TypeAnnotationPosition.emptyPath, null, tpidx, bndidx, pos);
  }

  /**
   * Create a method type parameter bound TypeAnnotationPosition.
   *
   * @param tpidx the type parameter index
   * @param bndidx the bound index
   * @param pos the source tree position
   * @return a method parameter TypeAnnotationPosition
   */
  public static TypeAnnotationPosition methodTypeParameterBoundTAPosition(
      final int tpidx, final int bndidx, final int pos) {
    return TypeAnnotationPosition.methodTypeParameterBound(
        TypeAnnotationPosition.emptyPath, null, tpidx, bndidx, pos);
  }

  /**
   * Copy a TypeAnnotationPosition.
   *
   * @param tapos the input TypeAnnotationPosition
   * @return a copied TypeAnnotationPosition
   */
  public static TypeAnnotationPosition copyTAPosition(final TypeAnnotationPosition tapos) {
    TypeAnnotationPosition res;
    switch (tapos.type) {
      case CAST:
        res =
            TypeAnnotationPosition.typeCast(
                tapos.location, tapos.onLambda, tapos.type_index, tapos.pos);
        break;
      case CLASS_EXTENDS:
        res =
            TypeAnnotationPosition.classExtends(
                tapos.location, tapos.onLambda, tapos.type_index, tapos.pos);
        break;
      case CLASS_TYPE_PARAMETER:
        res =
            TypeAnnotationPosition.typeParameter(
                tapos.location, tapos.onLambda, tapos.parameter_index, tapos.pos);
        break;
      case CLASS_TYPE_PARAMETER_BOUND:
        res =
            TypeAnnotationPosition.typeParameterBound(
                tapos.location,
                tapos.onLambda,
                tapos.parameter_index,
                tapos.bound_index,
                tapos.pos);
        break;
      case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
        res =
            TypeAnnotationPosition.constructorInvocationTypeArg(
                tapos.location, tapos.onLambda, tapos.type_index, tapos.pos);
        break;
      case CONSTRUCTOR_REFERENCE:
        res = TypeAnnotationPosition.constructorRef(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
        res =
            TypeAnnotationPosition.constructorRefTypeArg(
                tapos.location, tapos.onLambda, tapos.type_index, tapos.pos);
        break;
      case EXCEPTION_PARAMETER:
        res = TypeAnnotationPosition.exceptionParameter(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case FIELD:
        res = TypeAnnotationPosition.field(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case INSTANCEOF:
        res = TypeAnnotationPosition.instanceOf(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case LOCAL_VARIABLE:
        res = TypeAnnotationPosition.localVariable(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case METHOD_FORMAL_PARAMETER:
        res =
            TypeAnnotationPosition.methodParameter(
                tapos.location, tapos.onLambda, tapos.parameter_index, tapos.pos);
        break;
      case METHOD_INVOCATION_TYPE_ARGUMENT:
        res =
            TypeAnnotationPosition.methodInvocationTypeArg(
                tapos.location, tapos.onLambda, tapos.type_index, tapos.pos);
        break;
      case METHOD_RECEIVER:
        res = TypeAnnotationPosition.methodReceiver(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case METHOD_REFERENCE:
        res = TypeAnnotationPosition.methodRef(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case METHOD_REFERENCE_TYPE_ARGUMENT:
        res =
            TypeAnnotationPosition.methodRefTypeArg(
                tapos.location, tapos.onLambda, tapos.type_index, tapos.pos);
        break;
      case METHOD_RETURN:
        res = TypeAnnotationPosition.methodReturn(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case METHOD_TYPE_PARAMETER:
        res =
            TypeAnnotationPosition.methodTypeParameter(
                tapos.location, tapos.onLambda, tapos.parameter_index, tapos.pos);
        break;
      case METHOD_TYPE_PARAMETER_BOUND:
        res =
            TypeAnnotationPosition.methodTypeParameterBound(
                tapos.location,
                tapos.onLambda,
                tapos.parameter_index,
                tapos.bound_index,
                tapos.pos);
        break;
      case NEW:
        res = TypeAnnotationPosition.newObj(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case RESOURCE_VARIABLE:
        res = TypeAnnotationPosition.resourceVariable(tapos.location, tapos.onLambda, tapos.pos);
        break;
      case THROWS:
        res =
            TypeAnnotationPosition.methodThrows(
                tapos.location, tapos.onLambda, tapos.type_index, tapos.pos);
        break;
      case UNKNOWN:
      default:
        throw new BugInCF("Unexpected target type: " + tapos + " at " + tapos.type);
    }
    return res;
  }

  /**
   * Remove type annotations from the given type.
   *
   * @param in the input type
   * @return the same underlying type, but without type annotations
   */
  public static Type unannotatedType(final TypeMirror in) {
    final Type impl = (Type) in;
    if (impl.isPrimitive()) {
      // TODO: file an issue that stripMetadata doesn't work for primitives.
      // See eisop/checker-framework issue #21.
      return impl.baseType();
    } else {
      return impl.stripMetadata();
    }
  }
}
