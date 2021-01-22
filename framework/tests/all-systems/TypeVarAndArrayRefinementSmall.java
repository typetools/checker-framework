public class TypeVarAndArrayRefinementSmall {
    private <T extends Enum<T>> T getEnumValue(T[] constants) {
        for (T constant : constants) {
            return constant;
        }
        throw new Error();
    }
}
