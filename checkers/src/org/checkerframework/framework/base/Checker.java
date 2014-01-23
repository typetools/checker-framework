package org.checkerframework.framework.base;

public abstract class Checker<Q> {
    private QualifiedTypeFactory<Q> typeFactory;

    protected abstract QualifiedTypeFactory<Q> createTypeFactory();

    public final QualifiedTypeFactory<Q> getTypeFactory() {
        if (this.typeFactory == null) {
            this.typeFactory = createTypeFactory();
        }
        return this.typeFactory;
    }
}
