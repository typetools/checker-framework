import checkers.regex.quals.Regex;
import java.util.*;

class TypeParamSubtype {
    <T extends @Regex String> void nullRegexSubtype(Collection<T> col) {
        //:: (type.incompatible)
        col.add(null);
    }

    <T extends String> void nullSimpleSubtype(Collection<T> col) {
        //:: (type.incompatible)
        col.add(null);
    }

    <T extends @Regex String, U extends T> void nullRegexSubtype(Collection<T> col, U u) {
        col.add(u);
    }

    <T extends String, U extends T> void nullSimpleSubtype(Collection<T> col, U u) {
        col.add(u);
    }

}
