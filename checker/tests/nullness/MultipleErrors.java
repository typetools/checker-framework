// Make sure that errors in multiple types in
// the same compilation unit are all shown.

class MultipleErrors1 {
    // :: error: (assignment.type.incompatible)
    Object o1 = null;
}

class MultipleErrors2 {
    // :: error: (assignment.type.incompatible)
    Object o2 = null;
}

interface MultipleErrors3 {
    // :: error: (assignment.type.incompatible)
    Object o3 = null;
}
