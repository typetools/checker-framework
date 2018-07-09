package org.checkerframework.javacutil;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

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
     * @return true, iff a TypeCompound equal to tc is contained in list
     */
    public static boolean isTypeCompoundContained(
            List<TypeCompound> list, TypeCompound tc, Types types) {
        for (Attribute.TypeCompound rawat : list) {
            if (contentEquals(rawat.type.tsym.name, tc.type.tsym.name)
                    // TODO: in previous line, it would be nicer to use reference equality:
                    //   rawat.type == tc.type &&
                    // or at least "isSameType":
                    //   types.isSameType(rawat.type, tc.type) &&
                    // but each fails in some cases.
                    && rawat.values.equals(tc.values)
                    && isSameTAPositionExceptTreePos(rawat.position, tc.position)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contentEquals(Name n1, Name n2) {
        // Views of underlying bytes, not copies as with Name#contentEquals
        ByteBuffer b1 = ByteBuffer.wrap(n1.getByteArray(), n1.getByteOffset(), n1.getByteLength());
        ByteBuffer b2 = ByteBuffer.wrap(n2.getByteArray(), n2.getByteOffset(), n2.getByteLength());

        return b1.equals(b2);
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

    public static boolean isSameTAPositionExceptTreePos(
            TypeAnnotationPosition p1, TypeAnnotationPosition p2) {
        boolean eiequal = false;
        try {
            Field ei = TypeAnnotationPosition.class.getDeclaredField("exception_index");
            // TODO: ugly way to get access to field in JDK 9.
            // isSameTAPositionExceptTreePos also needs JDK 8/9 versions.
            ei.setAccessible(true);
            eiequal = ei.getInt(p1) == ei.getInt(p2);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            return false;
        }
        return eiequal
                && p1.isValidOffset == p2.isValidOffset
                && p1.bound_index == p2.bound_index
                && p1.location.equals(p2.location)
                && Arrays.equals(p1.lvarIndex, p2.lvarIndex)
                && Arrays.equals(p1.lvarLength, p2.lvarLength)
                && Arrays.equals(p1.lvarOffset, p2.lvarOffset)
                && p1.offset == p2.offset
                && p1.onLambda == p2.onLambda
                && p1.parameter_index == p2.parameter_index
                && p1.type == p2.type
                && p1.type_index == p2.type_index;
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
            Attribute attribute =
                    attributeFromAnnotationValue(entry.getKey(), entry.getValue(), env);
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
            Attribute attribute =
                    attributeFromAnnotationValue(entry.getKey(), entry.getValue(), env);
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
            }

            assert false : "Unexpected type of TypeMirror: " + t.getClass();
            return null;
        }

        @Override
        public Attribute visitEnumConstant(VariableElement c, Void p) {
            if (c instanceof Symbol.VarSymbol) {
                Symbol.VarSymbol sym = (Symbol.VarSymbol) c;
                if (sym.getKind() == ElementKind.ENUM_CONSTANT) {
                    return new Attribute.Enum(sym.type, sym);
                }
            }

            assert false : "Unexpected type of VariableElement: " + c.getClass();
            return null;
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
            assert false : "Unexpected type of AnnotationValue: " + av.getClass();
            return null;
        }
    }

    public static TypeAnnotationPosition unknownTAPosition() {
        return TypeAnnotationPosition.unknown;
    }

    public static TypeAnnotationPosition methodReturnTAPosition(final int pos) {
        return TypeAnnotationPosition.methodReturn(pos);
    }

    public static TypeAnnotationPosition methodReceiverTAPosition(final int pos) {
        return TypeAnnotationPosition.methodReceiver(pos);
    }

    public static TypeAnnotationPosition methodParameterTAPosition(final int pidx, final int pos) {
        return TypeAnnotationPosition.methodParameter(pidx, pos);
    }

    public static TypeAnnotationPosition methodThrowsTAPosition(final int tidx, final int pos) {
        return TypeAnnotationPosition.methodThrows(
                TypeAnnotationPosition.emptyPath, null, tidx, pos);
    }

    public static TypeAnnotationPosition fieldTAPosition(final int pos) {
        return TypeAnnotationPosition.field(pos);
    }

    public static TypeAnnotationPosition classExtendsTAPosition(final int implidx, final int pos) {
        return TypeAnnotationPosition.classExtends(implidx, pos);
    }

    public static TypeAnnotationPosition typeParameterTAPosition(final int tpidx, final int pos) {
        return TypeAnnotationPosition.typeParameter(
                TypeAnnotationPosition.emptyPath, null, tpidx, pos);
    }

    public static TypeAnnotationPosition methodTypeParameterTAPosition(
            final int tpidx, final int pos) {
        return TypeAnnotationPosition.methodTypeParameter(
                TypeAnnotationPosition.emptyPath, null, tpidx, pos);
    }

    public static TypeAnnotationPosition typeParameterBoundTAPosition(
            final int tpidx, final int bndidx, final int pos) {
        return TypeAnnotationPosition.typeParameterBound(
                TypeAnnotationPosition.emptyPath, null, tpidx, bndidx, pos);
    }

    public static TypeAnnotationPosition methodTypeParameterBoundTAPosition(
            final int tpidx, final int bndidx, final int pos) {
        return TypeAnnotationPosition.methodTypeParameterBound(
                TypeAnnotationPosition.emptyPath, null, tpidx, bndidx, pos);
    }

    public static TypeAnnotationPosition copyTAPosition(final TypeAnnotationPosition tapos) {
        try {
            Constructor<TypeAnnotationPosition> c =
                    TypeAnnotationPosition.class.getDeclaredConstructor(
                            TargetType.class,
                            int.class,
                            int.class,
                            JCLambda.class,
                            int.class,
                            int.class,
                            com.sun.tools.javac.util.List.class);
            c.setAccessible(true);
            TypeAnnotationPosition res =
                    c.newInstance(
                            tapos.type,
                            tapos.pos,
                            tapos.parameter_index,
                            tapos.onLambda,
                            tapos.type_index,
                            tapos.bound_index,
                            List.from(tapos.location));
            res.isValidOffset = tapos.isValidOffset;
            Field ei = TypeAnnotationPosition.class.getDeclaredField("exception_index");
            ei.setAccessible(true);
            ei.setInt(res, ei.getInt(tapos));
            // TODO: would be cleaner to use getter/setter, but something is wrong with:
            // TypeAnnotationPosition.class
            //     .getDeclaredMethod("setExceptionIndex", int.class).invoke(
            //         res, TypeAnnotationPosition.class
            //             .getDeclaredMethod("getExceptionIndex").invoke(tapos));
            res.location = List.from(tapos.location);
            if (tapos.lvarIndex != null) {
                res.lvarIndex = Arrays.copyOf(tapos.lvarIndex, tapos.lvarIndex.length);
            }
            if (tapos.lvarLength != null) {
                res.lvarLength = Arrays.copyOf(tapos.lvarLength, tapos.lvarLength.length);
            }
            if (tapos.lvarOffset != null) {
                res.lvarOffset = Arrays.copyOf(tapos.lvarOffset, tapos.lvarOffset.length);
            }
            res.offset = tapos.offset;
            return res;
        } catch (Throwable t) {
            assert false : "Checker Framework internal error: " + t;
            t.printStackTrace();
            return null;
        }
    }

    public static Type unannotatedType(final TypeMirror in) {
        final Type impl = (Type) in;
        return impl.stripMetadata();
    }
}
