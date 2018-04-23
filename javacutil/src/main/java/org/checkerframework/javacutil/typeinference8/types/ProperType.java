package org.checkerframework.javacutil.typeinference8.types;

public interface ProperType extends AbstractType {

    @Override
    default Kind getKind() {
        return Kind.PROPER;
    }

    ProperType boxType();

    boolean isSubType(ProperType superType);

    boolean isSubTypeUnchecked(ProperType superType);

    boolean isAssignable(ProperType superType);
}
