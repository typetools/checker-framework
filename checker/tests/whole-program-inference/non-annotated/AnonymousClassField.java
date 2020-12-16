// A test that ensures that stub-based inference correctly handles fields
// with inferred types that are anonymous classes.

import java.util.*;

class AnonymousClassField {
    public static final List foo = new ArrayList<String>() {};
}
