import java.util.LinkedList;
import java.util.List;
import org.checkerframework.framework.testchecker.util.*;

public class InvariantArrays {
  Object[] oa;
  @Encrypted Object[] eoa;

  String[] sa;
  @Encrypted String[] esa;

  void tests() {
    // TODOINVARR:: error: (assignment)
    oa = eoa;
    // This error only occurs with the Encrypted type system;
    // other type systems don't suffer an error here.
    // :: error: (assignment)
    eoa = oa;
    // TODOINVARR:: error: (assignment)
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

    // TODOINVARR:: error: (assignment)
    loa = eloa;
    // TODOINVARR:: error: (assignment)
    loa = ellra;
    // :: error: (assignment)
    eleoa = eloa;
    // TODOINVARR:: error: (assignment)
    leoa = eleoa;
  }
}
