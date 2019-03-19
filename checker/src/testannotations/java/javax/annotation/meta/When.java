package javax.annotation.meta;

/**
 * Used to describe the relationship between a qualifier T and the set of values S possible on an
 * annotated element.
 */
public enum When {
    /** S is a subset of T */
    ALWAYS,
    /** nothing definitive is known about the relation between S and T */
    UNKNOWN,
    /** S intersection T is non empty and S - T is nonempty. */
    MAYBE,
    /** S intersection T is empty. */
    NEVER;
}
