public class TypeVarAndArrayRefinement {

    private <T extends Enum<T>> T getEnumValue(Class<T> enumType, String name) {
        T[] constants = enumType.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException(enumType.getName() + " is not an enum type");
        }
        // previously the constants method was considered nullable mainly because it was an invalid
        // type because when lubbing type variables we didn't copy the declared types on the bounds
        // over to the lub
        for (T constant : constants) {
            if (constant.name().equalsIgnoreCase(name.replace('-', '_'))) {
                return constant;
            }
        }
        // same error that's thrown by Enum.valueOf()
        throw new IllegalArgumentException(
                "No enum constant " + enumType.getCanonicalName() + "." + name);
    }
}
