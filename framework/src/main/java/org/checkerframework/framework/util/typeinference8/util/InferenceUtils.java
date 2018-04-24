package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import org.checkerframework.javacutil.TreeUtils;

public class InferenceUtils {

    /**
     * Returns a mapping of type variable to type argument computed using the type of {@code
     * methodInvocationTree} and the return type of {@code methodType}.
     */
    public static Map<TypeVariable, TypeMirror> getMappingFromReturnType(
            ExpressionTree methodInvocationTree,
            ExecutableType methodType,
            ProcessingEnvironment env) {
        TypeMirror methodCallType = TreeUtils.typeOf(methodInvocationTree);
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) env;
        Types types = Types.instance(javacEnv.getContext());
        GetMapping mapping = new GetMapping(methodType.getTypeVariables(), types);
        mapping.visit(methodType.getReturnType(), methodCallType);
        return mapping.subs;
    }

    /**
     * Helper class for {@link #getMappingFromReturnType(ExpressionTree, ExecutableType,
     * ProcessingEnvironment)}
     */
    private static class GetMapping implements TypeVisitor<Void, TypeMirror> {

        final Map<TypeVariable, TypeMirror> subs = new HashMap<>();
        final List<? extends TypeVariable> typeVariables;
        final Types types;

        public GetMapping(List<? extends TypeVariable> typeVariables, Types types) {
            this.typeVariables = typeVariables;
            this.types = types;
        }

        @Override
        public Void visit(TypeMirror t, TypeMirror mirror) {
            if (t == null || mirror == null) {
                return null;
            }
            return t.accept(this, mirror);
        }

        @Override
        public Void visit(TypeMirror t) {
            return null;
        }

        @Override
        public Void visitPrimitive(PrimitiveType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitNull(NullType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitArray(ArrayType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.ARRAY : mirror;
            return visit(t.getComponentType(), ((ArrayType) mirror).getComponentType());
        }

        @Override
        public Void visitDeclared(DeclaredType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.DECLARED : mirror;
            DeclaredType param = (DeclaredType) mirror;
            if (types.isSubtype((Type) mirror, (Type) param)) {
                param = (DeclaredType) types.asSuper((Type) mirror, ((Type) param).asElement());
            }
            if (t.getTypeArguments().size() == param.getTypeArguments().size()) {
                for (int i = 0; i < t.getTypeArguments().size(); i++) {
                    visit(t.getTypeArguments().get(i), param.getTypeArguments().get(i));
                }
            }
            return null;
        }

        @Override
        public Void visitError(ErrorType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, TypeMirror mirror) {
            if (typeVariables.contains(t)) {
                subs.put(t, mirror);
            } else if (mirror.getKind() == TypeKind.TYPEVAR) {
                TypeVariable param = (TypeVariable) mirror;
                visit(t.getUpperBound(), param.getUpperBound());
                visit(t.getLowerBound(), param.getLowerBound());
            }
            // else it's not a method type variable
            return null;
        }

        @Override
        public Void visitWildcard(WildcardType t, TypeMirror mirror) {
            if (mirror.getKind() == TypeKind.WILDCARD) {
                WildcardType param = (WildcardType) mirror;
                visit(t.getExtendsBound(), param.getExtendsBound());
                visit(t.getSuperBound(), param.getSuperBound());
            } else if (mirror.getKind() == TypeKind.TYPEVAR) {
                TypeVariable param = (TypeVariable) mirror;
                visit(t.getExtendsBound(), param.getUpperBound());
                visit(t.getSuperBound(), param.getLowerBound());
            } else {
                assert false : mirror;
            }
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitNoType(NoType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitUnknown(TypeMirror t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitUnion(UnionType t, TypeMirror mirror) {
            return null;
        }

        @Override
        public Void visitIntersection(IntersectionType t, TypeMirror mirror) {
            assert mirror.getKind() == TypeKind.INTERSECTION : mirror;
            IntersectionType param = (IntersectionType) mirror;
            assert t.getBounds().size() == param.getBounds().size();

            for (int i = 0; i < t.getBounds().size(); i++) {
                visit(t.getBounds().get(i), param.getBounds().get(i));
            }

            return null;
        }
    }
}
