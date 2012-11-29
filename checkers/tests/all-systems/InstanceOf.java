
/* This example causes an error when computing the GLB of two types
 * because the GLB is empty. */

class FieldInstruction {}

class GETFIELD extends FieldInstruction {}

class PUTFIELD extends FieldInstruction {}

class InstanceOf {
    void test_two(FieldInstruction f) {
        if (f instanceof GETFIELD || f instanceof PUTFIELD) {
            if (f instanceof PUTFIELD) {
                return;
            }
            return;
        }
    }
}


