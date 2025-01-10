import java.util.List;
import java.util.Optional;
import org.checkerframework.dataflow.qual.*;

/**
 * Test case for rule #3: "Prefer alternative APIs over Optional.isPresent() and Optional.get()."
 */
public class Marks3a {

  class Customer {
    int getID() {
      return 42;
    }

    @Pure
    String getName() {
      return "Fozzy Bear";
    }

    String getNameImpure() {
      System.out.println("Side effect");
      return "Fozzy Bear";
    }
  }

  String customerNameByID_acceptable(List<Customer> custList, int custID) {
    Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

    // Not valid to report a map.and.orelse warning here
    String s = opt.isPresent() ? opt.get().getNameImpure() : "UNKNOWN";

    // :: warning: (prefer.map.and.orelse)
    return opt.isPresent() ? opt.get().getName() : "UNKNOWN";
  }

  String customerNameByID_acceptable2(List<Customer> custList, int custID) {
    Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

    // :: warning: (prefer.map.and.orelse)
    return !opt.isPresent() ? "UNKNOWN" : opt.get().getName();
  }

  String customerNameByID_acceptable3(List<Customer> custList, int custID) {
    Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

    String customerName;
    // :: warning: (prefer.map.and.orelse)
    if (opt.isPresent()) {
      customerName = opt.get().getName();
    } else {
      customerName = "UNKNOWN";
    }

    return customerName;
  }

  String customerNameByID_acceptable4(List<Customer> custList, int custID) {
    Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

    String customerName = "";
    String unknownCustomerName = "";

    // This is OK, because the two LHSes differ.
    if (opt.isPresent()) {
      customerName = opt.get().getName();
    } else {
      unknownCustomerName = "UNKNOWN";
    }

    return customerName;
  }

  String customerNameByID_better(List<Customer> custList, int custID) {
    Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

    return opt.map(Customer::getName).orElse("UNKNOWN");
  }
}
