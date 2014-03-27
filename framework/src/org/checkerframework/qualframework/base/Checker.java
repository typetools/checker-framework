package org.checkerframework.qualframework.base;

/** Main entry point for a pluggable type system.  Each type system must
 * provide an implementation of this abstract class that produces an
 * appropriate {@link QualifiedTypeFactory} for the type system.
 */
public abstract class Checker<Q> {
    private QualifiedTypeFactory<Q> typeFactory;

    /**
     * Constructs the {@link QualifiedTypeFactory} for use by this {@link
     * Checker}. 
     */
    protected abstract QualifiedTypeFactory<Q> createTypeFactory();

    /**
     * Returns the {@link QualifiedTypeFactory} used by this {@link Checker}.
     */
    public final QualifiedTypeFactory<Q> getTypeFactory() {
        if (this.typeFactory == null) {
            this.typeFactory = createTypeFactory();
        }
        return this.typeFactory;
    }

    // TODO: support for checker-defined visitor
}
