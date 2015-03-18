package org.checkerframework.qualframework.base;

import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedArrayType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedIntersectionType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedNoType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedNullType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedParameterDeclaration;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedPrimitiveType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeDeclaration;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedUnionType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedWildcardType;


/** Visitor that replaces the qualifier of a {@link QualifiedTypeMirror}.
 */
public class SetQualifierVisitor<Q> implements QualifiedTypeVisitor<Q, QualifiedTypeMirror<Q>, Q> {
    // This class has no fields that refer to `Q`, so all instances have the
    // same representation.  That means we can reuse a single instance for all
    // instantiations of `Q`.
    @SuppressWarnings("rawtypes")
    private static SetQualifierVisitor INSTANCE = new SetQualifierVisitor();

    /**
     * Get an instance of {@code SetQualifierVisitor}.  This method may be more
     * efficient than calling the constructor, since it may reuse instances
     * sometimes.
     */
    @SuppressWarnings("unchecked")
    public static <Q> SetQualifierVisitor<Q> getInstance() {
        return (SetQualifierVisitor<Q>)INSTANCE;
    }

    /**
     * Helper method to apply a {@code SetQualifierVisitor} to a {@link
     * QualifiedTypeMirror}.
     *
     * Calling {@code apply(...)} is like {@code getInstance().visit(...)}, but
     * doesn't require an explicit type argument like {@code getInstance}
     * does.
     */
    public static <Q> QualifiedTypeMirror<Q> apply(QualifiedTypeMirror<Q> type, Q newQual) {
        return SetQualifierVisitor.<Q>getInstance().visit(type, newQual);
    }


    @Override
    public QualifiedTypeMirror<Q> visit(QualifiedTypeMirror<Q> type) {
        return visit(type, null);
    }

    @Override
    public QualifiedTypeMirror<Q> visit(QualifiedTypeMirror<Q> type, Q newQual) {
        if (type == null) {
            return null;
        }
        return type.accept(this, newQual);
    }


    @Override
    public QualifiedTypeMirror<Q> visitArray(QualifiedArrayType<Q> type, Q newQual) {
        return new QualifiedArrayType<Q>(type.getUnderlyingType(),
                newQual,
                type.getComponentType()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitDeclared(QualifiedDeclaredType<Q> type, Q newQual) {
        return new QualifiedDeclaredType<Q>(type.getUnderlyingType(),
                newQual,
                type.getTypeArguments()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitExecutable(QualifiedExecutableType<Q> type, Q newQual) {
        return new QualifiedExecutableType<Q>(type.getUnderlyingType(),
                type.getParameterTypes(),
                type.getReceiverType(),
                type.getReturnType(),
                type.getThrownTypes(),
                type.getTypeParameters()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitIntersection(QualifiedIntersectionType<Q> type, Q newQual) {
        return new QualifiedIntersectionType<Q>(type.getUnderlyingType(),
                newQual,
                type.getBounds()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitNoType(QualifiedNoType<Q> type, Q newQual) {
        return new QualifiedNoType<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitNull(QualifiedNullType<Q> type, Q newQual) {
        return new QualifiedNullType<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitPrimitive(QualifiedPrimitiveType<Q> type, Q newQual) {
        return new QualifiedPrimitiveType<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeVariable(QualifiedTypeVariable<Q> type, Q newQual) {
        return new QualifiedTypeVariable<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitUnion(QualifiedUnionType<Q> type, Q newQual) {
        return new QualifiedUnionType<Q>(type.getUnderlyingType(),
                newQual,
                type.getAlternatives()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitWildcard(QualifiedWildcardType<Q> type, Q newQual) {
        return new QualifiedWildcardType<Q>(type.getUnderlyingType(),
                type.getExtendsBound(),
                type.getSuperBound()
                );
    }


    @Override
    public QualifiedTypeMirror<Q> visitTypeDeclaration(QualifiedTypeDeclaration<Q> type, Q newQual) {
        return new QualifiedTypeDeclaration<Q>(type.getUnderlyingType(),
                newQual,
                type.getTypeParameters()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitParameterDeclaration(QualifiedParameterDeclaration<Q> type, Q newQual) {
        return new QualifiedParameterDeclaration<Q>(type.getUnderlyingType());
    }
}
