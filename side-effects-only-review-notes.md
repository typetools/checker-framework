# Review notes

## 4. Alias union-find is flow-insensitive and monotonic, which suppresses errors

`DisallowedSideEffects.aliasedExpressions` only ever merges sets; it never splits, and it is
populated during the same pre-order traversal that performs the checks.

```java
@SideEffectsOnly("#1")
void m(List<String> a, List<String> b, List<String> c) {
  List<String> t = a;
  t = b;        // now a, b, t are all one alias set
  t.add("x");   // accepted, because the set contains `a`
}
```

Mutating `b` is accepted because `b` was transitively merged with `a`. This is the unsound
direction: legitimate errors are silently suppressed. It is also the mechanism that makes
`checker/tests/sideeffectsonly/NestedSideEffectsWithAliasing.java` pass, so that test is not
distinguishing "correct alias reasoning" from "over-merging."

A flow-insensitive may-alias approximation is a defensible starting point for a first cut, but it
should be a documented limitation rather than an implicit one.

**Status:** documented, not fixed. The caveat is in the `SideEffectsOnly` javadoc and, as of this
round, in `advanced-features.tex` under "Checking `@SideEffectsOnly`", with the worked example
above. A real fix means making the alias analysis flow-sensitive, which is a redesign rather than
a patch.

---

## 5. Unannotated callees

**5a is fixed.** `DisallowedSideEffects.visitMethodInvocation` now reports
`purity.unknown.sideeffectsonly` for *every* call to a method that is not `@Pure`,
`@SideEffectFree`, or `@SideEffectsOnly`, not only when the call has no receiver or arguments. This
mirrors how `@SideEffectFree` is checked (`purity.not.sideeffectfree.call`) and closes the hole
where an unannotated callee was assumed to modify only its receiver and arguments.

The blast radius was far smaller than expected: the annotated JDK already writes
`@SideEffectsOnly("this")` on the mutating `Collection` methods, so the whole `sideeffectsonly`
suite kept passing with exactly one new diagnostic.

**Residual, and the reason this item stays open:** that one new diagnostic is in
`checker/tests/sideeffectsonly/StaticIteratorSE.java`, on `e.nextElement()`. `Enumeration` is not
annotated in the annotated JDK, so `Iterator`/`Enumeration` — the feature's own motivating example
— cannot be written without a suppression. The strict rule is only as usable as the JDK
annotations behind it. Annotating `Enumeration` (and auditing the rest of `java.util`) is follow-up
work in the `jdk` repository, not this one.

**5b is fixed.** When the callee *is* annotated `@SideEffectsOnly`, the side-effected expressions
now come from the callee's own annotation, viewpoint-adapted to the call site, rather than from the
receiver-and-arguments list. This removed the suppression in
`checker/tests/nullness/generics/MyMap.java`: `putAll` calls `put(entry.getKey(), ...)`, and `put`
is `@SideEffectsOnly("this")`, so the arguments are no longer assumed to be modified.

---

## Constructors: no use-site effect

`CFAbstractTransfer.visitObjectCreation` never calls `store.updateForMethodCall`, so a
constructor's `@SideEffectsOnly` annotation is verified at its declaration but does not affect type
refinement at a `new` expression. This is now stated in the `SideEffectsOnly` javadoc and the
manual, and marked with a `TODO` at the call site, but it is not implemented.
