package checkers.flow.analysis.checkers;


/**
 * The default abstract value used in the Checker Framework.
 *
 * @author Stefan Heule
 *
 */
public class CFValue extends CFAbstractValue<CFValue> {

    public CFValue(CFAbstractAnalysis<CFValue, ?, ?> analysis,
            InferredAnnotation[] annotations) {
        super(analysis, annotations);
    }

}
