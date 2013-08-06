
import java.util.*;

class Component {}

// If this class inherits from the raw type ArrayList, then we
// get an error type as the GLB below.
class Components extends ArrayList {}

// If we include a type parameter in the superclass, then there
// is no error below.
// class Components extends ArrayList<Component> {}

public class RawTypeAssignment {
    static Components getComponents() {
        return new Components();
    }

    static void addTimes(Calendar calendar) {
        // In this method, we compute the greatest lower bound of
        // ArrayList<Component> and the return type of getComponents.
        // Currently, this GLB is an error type, which is a bug.
        //:: warning: [unchecked] unchecked conversion
        ArrayList<Component> clist = getComponents();
        clist.get(0);
    }
}
