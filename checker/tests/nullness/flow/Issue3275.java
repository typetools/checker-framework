// Test case for issue #3275:
// https://github.com/typetools/checker-framework/issues/3275

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3275 {
  public @NonNull Object f = new Object();
  public boolean b = false;

  void return_n(@Nullable Object obj) {
    if (obj != null) {
      obj.toString();
    }
  }

  void return_np(@Nullable Object obj) {
    if ((obj != null)) {
      obj.toString();
    }
  }

  void return_en(@Nullable Object obj) {
    if (!(obj == null)) {
      obj.toString();
    }
  }

  void return_eet(@Nullable Object obj) {
    if ((obj == null) == true) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void return_eef(@Nullable Object obj) {
    if ((obj == null) == false) {
      obj.toString();
    }
  }

  void return_eeb(@Nullable Object obj) {
    if ((obj == null) == b) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void return_ent(@Nullable Object obj) {
    if ((obj == null) != true) {
      obj.toString();
    }
  }

  void return_enf(@Nullable Object obj) {
    if ((obj == null) != false) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void return_enb(@Nullable Object obj) {
    if ((obj == null) != b) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void return_net(@Nullable Object obj) {
    if ((obj != null) == true) {
      obj.toString();
    }
  }

  void return_nef(@Nullable Object obj) {
    if ((obj != null) == false) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void return_neb(@Nullable Object obj) {
    if ((obj != null) == b) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void return_nnt(@Nullable Object obj) {
    if ((obj != null) != true) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void return_nnf(@Nullable Object obj) {
    if ((obj != null) != false) {
      obj.toString();
    }
  }

  void return_nnb(@Nullable Object obj) {
    if ((obj != null) != b) {
      // :: error: (dereference.of.nullable)
      obj.toString();
    }
  }

  void assign_n(@Nullable Object obj) {
    if (obj != null) {
      f = obj;
    }
  }

  void assign_np(@Nullable Object obj) {
    if ((obj != null)) {
      f = obj;
    }
  }

  void assign_en(@Nullable Object obj) {
    if (!(obj == null)) {
      f = obj;
    }
  }

  void assign_eet(@Nullable Object obj) {
    if ((obj == null) == true) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }

  void assign_eef(@Nullable Object obj) {
    if ((obj == null) == false) {
      f = obj;
    }
  }

  void assign_eeb(@Nullable Object obj) {
    if ((obj == null) == b) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }

  void assign_ent(@Nullable Object obj) {
    if ((obj == null) != true) {
      f = obj;
    }
  }

  void assign_enf(@Nullable Object obj) {
    if ((obj == null) != false) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }

  void assign_enb(@Nullable Object obj) {
    if ((obj == null) != b) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }

  void assign_net(@Nullable Object obj) {
    if ((obj != null) == true) {
      f = obj;
    }
  }

  void assign_nef(@Nullable Object obj) {
    if ((obj != null) == false) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }

  void assign_neb(@Nullable Object obj) {
    if ((obj != null) == b) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }

  void assign_nnt(@Nullable Object obj) {
    if ((obj != null) != true) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }

  void assign_nnf(@Nullable Object obj) {
    if ((obj != null) != false) {
      f = obj;
    }
  }

  void assign_nnb(@Nullable Object obj) {
    if ((obj != null) != b) {
      // :: error: (assignment.type.incompatible)
      f = obj;
    }
  }
}
