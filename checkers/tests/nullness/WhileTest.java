import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.NonNull;

public class WhileTest {
    @Nullable Integer z;
    @NonNull Integer nnz;

    public static void main(String[] args) {
        new WhileTest().testwhile1();
    }
    
    public void testwhile1() {
        z = null;
        //:: (assignment.type.incompatible)
        nnz = z;

        while (z == null) {
            break;
        }
        //:: (assignment.type.incompatible)
        nnz = z;
        nnz.toString();
    }

    public void testwhile2() {
        z = null;
        while (z == null) {
            ;
        }
        nnz = z;
    }
    
    public void testdo1() {
        z = null;
        do {
            break;
        } while (z == null);
        //:: (assignment.type.incompatible)
        nnz = z;
    }

    public void testdo2() {
        z = null;
        do {
            ;
        } while (z == null);
        nnz = z;
    }

    public void testfor1() {
        z = null;
        for(;z==null;) {
            break;
        }
        //:: (assignment.type.incompatible)
        nnz = z;
    }

    public void testfor2() {
        z = null;
        for(;z==null;) {
            ;
        }
        nnz = z;
    }

}