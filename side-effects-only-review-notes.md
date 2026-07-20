# Review notes

## 4. Alias union-find is flow-insensitive and monotonic, which suppresses errors

`DisallowedSideEffects.DisallowedSideEffectsHelper.aliasedExpressions` only ever merges sets; it
never splits, and it is populated during the same pre-order traversal that performs the checks.

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
should be a documented limitation rather than an implicit one. I added a caveat to the
`SideEffectsOnly` javadoc; consider also adding one to `purity-checker.tex` or
`advanced-features.tex`, since that is where users will look.

---

## 5. A call is assumed to modify its receiver and arguments, whatever the callee says

`DisallowedSideEffects.visitMethodInvocation` reports `purity.unknown.sideeffectsonly` only when
`actualSideEffectedExprs.isEmpty()` — that is, only when the call has neither a receiver nor
arguments. Otherwise an unannotated callee is treated as modifying exactly its receiver and
arguments:

```java
@SideEffectsOnly("#1")
void m(Object o) {
  helper(o);   // helper is unannotated and assigns a static field — accepted
}
```

An unannotated method may modify static state and anything else not reachable from its receiver or
arguments. Since this declaration-site check is what is supposed to justify the use-site's
precision gain, the hole undercuts the feature's guarantee.

I added a `TODO` naming this at the call site. Fixing it properly means either treating any
unannotated call as modifying arbitrary state (likely too strict to be usable in practice, and it
would require annotating a great deal of the JDK first), or introducing a notion of "modifies at
most what is reachable from these expressions."

**Related, and a false positive rather than a hole:** `visitMethodInvocation` computes
`actualSideEffectedExprs` as the receiver plus all arguments *regardless of the callee's
annotation*, so even a callee that is annotated `@SideEffectsOnly` is assumed to modify its
arguments. `checker/tests/nullness/generics/MyMap.java` needs a suppression for exactly this:
`putAll` calls `put(entry.getKey(), entry.getValue())`, and `put` is `@SideEffectsOnly("this")`, so
it does not modify those arguments at all. When the callee is annotated, the side-effected
expressions should come from the annotation (view-adapted to the call site), not from the argument
list.

---

## 7. `DisallowedSideEffects` is over-built for its single use

The outer class holds one `List<IPair<Tree, JavaExpression>>`, an `addExpr`, a `getExprs`, and a
static entry point; the nested `DisallowedSideEffectsHelper` does all the work. The only consumer,
`checkSideEffectsOnly`, immediately unwraps the object via `getExprs()`.

Consider collapsing: let the helper accumulate the list directly and have `checkSideEffectsOnly`
report from it. That removes a type, a constructor, and two delegating methods.

If the wrapper stays, `getExprs()` should carry the codebase's usual note that the returned value
is aliased to internal state and should not be side-effected (compare
`CFAbstractStore.getFieldValues`).

---

## 8. Test coverage gaps

Present and good: nested field expressions, aliasing, multiple expressions, conflicting
annotations, empty annotation, unparseable expression at the declaration site, and precondition
interaction via the new `optional-side-effects` directory.

Missing:

- **Method references.** `InheritedSideEffectsOnly.java` now covers `checkPurity()`'s cases, but
  only via ordinary overriding. The method-reference path, which reports `purity.methodref`
  instead of `purity.overriding`, is still untested.
- **Use-site parse error.** `MalformedSideEffectsOnly.java` exercises only the declaration site.
  A test where the annotation parses at the declaration but not at a call site would cover
  `CFAbstractAnalysis.getSideEffectsOnlyExpressions`'s error path (now fail-closed).
- **Constructors.** `@Target` permits `CONSTRUCTOR`, but no test applies `@SideEffectsOnly` to one,
  and neither the manual nor the javadoc discusses what it means there.
