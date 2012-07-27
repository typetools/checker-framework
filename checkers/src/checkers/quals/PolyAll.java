package checkers.quals;

import java.lang.annotation.*;

import checkers.util.QualifierPolymorphism;
import checkers.nullness.quals.PolyNull;
import checkers.interning.quals.PolyInterned;

/**
 * A polymorphic type qualifier that varies over all type hierarchies.
 * <p>
 *
 * Writing <tt>@PolyAll</tt> is equivalent to writing a polymorphic
 * qualifier for every type system (whether such a qualifier has been
 * declared or not).
 * <p>
 * 
 * The <tt>@PolyAll</tt> annotation applies to every type qualifier hierarchy for
 * which no explicit qualifier is written.  For example, a declaration like
 * <tt>@PolyAll @NonNull String s</tt> is polymorphic over every type system
 * \emph{except} the nullness type system, for which the type is fixed at
 * <tt>@NonNull</tt>.
 * <p>
 *
 * The optional argument creates conceptually distinct polymorphic
 * qualifiers, permitting them to vary independently.  When a method has
 * multiple occurrences of a single polymorphic qualifier (all of them have
 * no argument or the same argument), all of those occurrences vary
 * together.
 * 
 * To support <tt>@PolyAll</tt> in a type system, simply add it to the
 * list of <tt>@TypeQualifiers</tt>.
 *
 * @see PolyNull
 * @see PolyInterned
 * @see PolymorphicQualifier
 * @see QualifierPolymorphism
 */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@PolymorphicQualifier
public @interface PolyAll {
    // TODO: support multiple variables using an id.
    //int value() default 0;
}
