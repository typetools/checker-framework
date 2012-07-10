import java.util.*;
import tests.util.*;

class InvariantArrays {
    Object [] oa;
    @Encrypted Object[] eoa;

    String [] sa;
    @Encrypted String[] esa;

    void tests() {
        //:: error: (assignment.type.incompatible)
        oa = eoa;
        //:: error: (assignment.type.incompatible)
        eoa = oa;
        //:: error: (assignment.type.incompatible)
        oa = esa;
        // OK
        oa = sa;
        eoa = esa;
    }

    List<? extends Object>[] loa;
    LinkedList<? extends Runnable>[] llra;
    List<? extends @Encrypted Object>[] leoa;
    LinkedList<? extends @Encrypted Runnable>[] llera;
    @Encrypted List<? extends Object>[] eloa;
    @Encrypted LinkedList<? extends Runnable>[] ellra;
    @Encrypted List<? extends @Encrypted Object>[] eleoa;
    @Encrypted LinkedList<? extends @Encrypted Runnable>[] ellera;

    void genericTests() {
        // OK
        loa = llra;
        loa = leoa;
        loa = llera;
        eloa = ellra;
        leoa = llera;
        eloa = ellera;

        //:: error: (assignment.type.incompatible)
        loa = eloa;
        //:: error: (assignment.type.incompatible)
        loa = ellra;
        //:: error: (assignment.type.incompatible)
        eleoa = eloa;
        //:: error: (assignment.type.incompatible)
        leoa = eleoa;
    }
}
