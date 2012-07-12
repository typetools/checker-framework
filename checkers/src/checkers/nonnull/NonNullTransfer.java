package checkers.nonnull;

import checkers.commitment.CommitmentTransfer;

/**
 * Transfer function for the non-null type system. Performs the following
 * refinements:
 * <ul>
 * <li>TODO: finish documentation
 * </ul>
 * 
 * @author Stefan Heule
 */
public class NonNullTransfer extends CommitmentTransfer<NonNullTransfer> {

    public NonNullTransfer(NonNullAnalysis analysis) {
        super(analysis);
    }
}
