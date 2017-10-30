/* This example causes an error when computing the GLB of two types
 * because the GLB is empty. */

class FieldInstruction {}

class GETFIELD extends FieldInstruction {}

class PUTFIELD extends FieldInstruction {}

class InstanceOf {
    public void emptyGLB(FieldInstruction f) {
        if (f instanceof GETFIELD || f instanceof PUTFIELD) {
            if (f instanceof PUTFIELD) {
                // During org.checkerframework.dataflow analysis, we can believe that f is both a
                // GETFIELD and a PUTFIELD, which yields an empty GLB.  Once
                // org.checkerframework.dataflow converges, it will know that f is a PUTFIELD.
                return;
            }
            return;
        }
    }

    public boolean assignInstanceOf(Object obj) {
        // We fixed a bug where the type in an instanceof expression
        // like Class<?> was stored as the abstract value result of
        // the expression.
        boolean is_class = obj instanceof Class<?>;
        return is_class;
    }
}
