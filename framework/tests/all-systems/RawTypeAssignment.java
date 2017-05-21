import java.util.ArrayList;
import java.util.Calendar;

class Component {}

class Components extends ArrayList {}

// If we include a type parameter in the superclass, then there
// is no error below.
// class Components extends ArrayList<Component> {}

@SuppressWarnings(
        "list.access.unsafe.high") // The Index Checker correctly issues this warning here.
public class RawTypeAssignment {
    static Components getComponents() {
        return new Components();
    }

    static void addTimes(Calendar calendar) {
        // Type systems may issue an error below because of a mismatch between the type arguments.
        @SuppressWarnings("assignment.type.incompatible")
        //:: warning: [unchecked] unchecked conversion
        ArrayList<Component> clist = getComponents();
        clist.get(0);
    }
}
