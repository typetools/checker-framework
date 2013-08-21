package checkers.igj;

/**
 * Provides some state for the {@code IGJVisitor}.
 * 
 * So far, it stores two objects: 1. The immutability of the scope the
 * visitor is in 2. Binding for resolving wildcards
 */
public class VisitorState {
    public IGJImmutability thisImmutability = IGJImmutability.READONLY;
}
