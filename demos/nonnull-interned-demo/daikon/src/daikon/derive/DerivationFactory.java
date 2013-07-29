package daikon.derive;

/**
 * Factory to create and describe derived variables.
 * DerivationFactory creates a Derivation[] per group of source
 * variables, and children of DerivationFactory create different kinds
 * of Derivation[] using instantiate().  DerivationFactory chooses
 * how many (if any) Derivations to instantiate (so calling classes
 * don't have to decide).
 * <p>
 * This class contains no
 * methods because UnaryDervationFactory and BinaryDerivationFactory
 * have instantiate() methods that take a different number of
 * arguments.
 **/

public interface DerivationFactory {

}
