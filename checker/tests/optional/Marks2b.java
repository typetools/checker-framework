import java.util.List;
import java.util.Optional;

public class Marks2b {

    class Customer {
        int getID() {
            return 42;
        }

        String getName() {
            return "Fozzy Bear";
        }
    }

    String customerNameByID(List<Customer> custList, int custID) {
        Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();
        //:: warning: (prefer.map.and.orelse)
        return opt.isPresent() ? opt.get().getName() : "UNKNOWN";
    }
}
