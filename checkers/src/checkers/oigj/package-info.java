package checkers.oigj;

/**
 * OIGJ is a Java-extension language that expresses ownership and immutability
 * constraints.
 *
 * The language has the following simple language rules:
 *
 * <ol>
 * <li><b>Ownership nesting</b>: The owner parameter of a type must
 * be lower or equal in the dominator tree compared to all other owner
 * parameters inside that type.</li>
 *
 * <li><b>FieldAssignment</b>: Field assignment {@code o.f = ...} is legal
 * iff:
 *     <ol type="i">
 *     <li>{@code I(o) <= AssignsFields} or {@code f} is annotated as
 *     {@code @Assignable}, and</li>
 *     <li>{@code o = this} or the type of {@code f} does not contain the
 *     owner {@code Dominator} or {@code Modifier}.
 *     </ol>
 * </ol>
 *
 */
