# Index checking for mutable length data structures

The [Index Checker](https://checkerframework.org/manual/#index-checker) is
currently restricted to fixed-size data structures. A fixed-size data
structure is one whose length cannot be changed once it is created, such
as arrays and `String`s. This limitation prevents the Index Checker from
verifying indexing operations on mutable-size data structures, like
`List`s, that have `add()` or `remove()` methods. Since this kind of
collection is common in practice, this is a severe limitation for the
Index Checker.

The fundamental problem is the combination of *mutation* (side effects) and
*aliasing*.  The Index Checker uses types that are dependent on the length
of data structures.  Suppose you have the declaration

```java
@LTLengthOf("myList") int i;
```

If variable `myList` is aliased to some other variable (that is, both
variables refer to the same Java list), then that other variable can change
the length of the list, and then the annotation might no longer be correct
about the integer `i`.  The Index Checker must ensure that all annotations
always state correct facts.

Since the problem is a combination of mutation and aliasing, overcoming the
problem can focus on the aliasing problem or the mutation problem.  Solving
the problem could involve forbidding certain code, or permitting the code
but improving the checker to precisely handle it.  For example, the current
Index Checker effectively forbids all mutable-length data structures, such
as `List`s.

## Focus on side effects: permit mutation, but invalidate flow facts when a list might be modified

It is essential to invalidate some flow facts when a mutation may occur.
(And to forbid mutation when prohibited by a programmer-written annotation.)
It is a goal to invalidate as few flow facts as possible, while still retaining soundness.

When an `int` is mutated, then no change is needed:  use the Index
Checker's current logic.  This section is about what to do when a `List` is
mutated.

Here are some steps toward invalidating fewer flow facts.

1. Invalidate all Index Checker qualifiers whenever a side effect may
   occur (for example, at every call to a method that might have side
   effects). This is imprecise (it loses refined type information) and
   therefore will result in many false positive alarms, so it is an
   impractical solution. However, it is sound, which is most important.
   Subsequent steps will improve its precision.

2. Only invalidate *some* Index Checker qualifiers when a side effect may
   occur -- namely, those relating to mutable-length data structures. This
   requires determining which indexable datatypes have mutable lengths,
   which can be hard-coded for the collection classes in the JDK.

3. Implement the
   [`@SideEffectsOnly`](https://rawgit.com/mernst/checker-framework/refs/heads/index-checker-mutable-project/docs/developer/new-contributor-projects.html#SideEffectsOnly)
   annotation.
   Suppose that a method is called that only side-effects variable `a` of type
   `T` and variable `b` of type `U`. Then the Index Checker needs to
   invalidate:

    * qualifiers on all expressions of type `T` or `U` (and their supertypes
      and subtypes). The Index Checker should retain qualifiers on types that
      are unrelated to `T` and `U`.
    * dependent type qualifiers that mention any expression of type `T` or
      `U`.

4. Devise and implement a new annotation (e.g., `@BackedBy`) that connects
   the length of a data structure to the length of its backing data
   structure. For example, an annotation on `ArrayList` could tell the checker
   which field of the `ArrayList` class is the backing array. Then, make it an
   error if any method of a class with such an annotation re-assigns the
   backing array unless it has a new annotation (e.g., `@ChangesLength`). This
   annotation could apply recursively, to allow data structures that are
   themselves backed by mutable-length data structures. Only invalidate facts
   about a mutable length data structure when one of its `@ChangesLength`
   methods is called.

   This proposal offers a way for users to extend the guarantees of the Index
   Checker to their own data structures. The `@BackedBy` annotation is useful
   *while checking the implementation of `ArrayList`*, because it allows the
   checker to issue a warning if there exists a method that modifies the
   length of the underlying data structure (i.e., `elementData` in the case of
   ArrayList) that does not have a corresponding annotation indicating that it
   modifies the length of the data structure being analyzed. In other words,
   `@BackedBy` is useful for specifying and verifying the *implementations* of
   data structures, rather than uses.  This should not be worked on until
   the type-checker is functional.  `@BackedBy` would need to be written on
   the implementation of `ArrayList` if we were to type-check it.
   But the main purpose of `@BackedBy` is to allow developers who write their
   own data structures to also enroll them into the Index Checker in a sound
   way.

5. The `@LengthOf` annotation is currently an alias for `@LTELengthOf`.
   But its definition is a value that indicates the size of a data
   structure. It can perhaps be used.

6. Run a pointer analysis before type-checking. Now, at a possible side
   effect to expressions `a` and `b`, it is only necessary to invalidate
   Index Checker qualifiers related to expressions that may be aliased to
   `a` or `b`. One possible pointer analysis is that of
   [Doop](https://github.com/plast-lab/doop-mirror).

   A significant downside to this approach is that it is a whole-program
   analysis rather than a modular one. A modular analysis works
   method-by-method. A whole-program analysis requires that the entire
   program is present to be analyzed, and it is slow.

7. Modify rather than discarding the Index Checker qualifiers on
   possibly-aliased data structures. If an expression is must-aliased to
   `a`, then its type should be updated in exactly the way that `a`'s is.
   If an expression is may-aliased to `a`, then its type should become the
   LUB of its old type and `a`'s new type. Adjusting the types of
   dependently-typed variables is a bit more complicated.

## Forbid problematic mutation: only permit lists to grow

If an index is valid for a given collection, then the index is also valid
for a bigger collection.  Adding to a collection does not invalidate any existing indices.
Therefore, the Index Checker can ignore code that can add to a collection, treating it
just like code that does not modify the collection at all.

As a first step, limit the guarantees provided by the checker only to
non-shrinking collections.  (Just as they are currently limited to
collections whose size never changes.)

### Qualifier hierarchy

This is the qualifier hierarchy:

``` text
@BottomGrowShrink <: @GrowOnly <: @UnshrinkableRef
@BottomGrowShrink <: @UncheckedShrinkable <: @Shrinkable <: @UnshrinkableRef
```

* A `@GrowOnly` reference to a collection states that as long as that reference exists,
  the size of the collection will not decrease (elements cannot be removed, but can be added).
  Calling `remove()`, `clear()`, etc. is forbidden, and no alias can remove
  elements, either.
  The expression is not aliased to any `@Shrinkable` list.
  Any valid index remains valid (unless the index is changed), regardless of
  changes to any list.
  This is the default type.
* A `@Shrinkable` reference to a collection allows removing elements
  from the collection using methods such as `remove()` and `clear()`.
  An alias to the collection may also shrink the collection.
* `@UnshrinkableRef`: calling `remove()`, `clear()`, etc. is forbidden.
  A `@UnshrinkableRef` reference to the collection cannot be used to remove elements,
  but admits the possibility to remove elements from the collection using another reference to it.
* The annotation `@UncheckedShrinkable` is like `@Shrinkable`,
  but is used to opt out of index checking.
  The checker behaves as if all indices are valid for this collection.

Unless a reference is `@GrowOnly` or `@UncheckedShrinkable`,
the checker has to invalidate all `@IndexFor` qualifiers for it
whenever a side effect may occur.

The type hierarchy guarantees that no `@GrowOnly` expression is
aliased to any `@Shrinkable` expression.

A collection allocated with `new @GrowOnly` or `new @Shrinkable`
will have indices checked.

When annotating a library, do not use `@UncheckedShrinkable` and `@GrowOnly`.
TODO: why not use `@GrowOnly`?

### Implementation strategy for `@Shrinkable` and `@UncheckedShrinkable`

One approach is to implement `@Shrinkable` and `@UncheckedShrinkable`.
Another approach is to initially implement only `@UncheckedShrinkable` (and
probably also treat `@UnshrinkableRef` as unchecked).

### Library annotations

As is typical, JDK annotations are trusted, not checked.

Checking indices of a mutable collection type (such as `List`) in the Java library would require annotating its methods:

* Methods that accept indices must have the parameters annotated `@IndexFor` or `@IndexOrHigh`. Missing annotation would create unsoundness.
* Methods that return indices should have the return type annotated `@IndexFor` or `@IndexOrHigh`. Missing annotation would cause false positives.
* Most methods do not remove from the collection -- the default qualifier for this type should be UnshrinkableRef.
* Methods that can remove from the collection must use the Shrinkable annotation. Missing annotation would create unsoundness.
* Methods that allocate and return a new list could also use the Shrinkable annotation.

### Annotating application code

In application code, each allocation of a list should be by default `@UncheckedShrinkable`.
If all lists are `@UncheckedShrinkable`, it would ideally result in no warnings reported.

Then, collections that are intended to be grow-only should be annotated `@GrowOnly`.
Now, the Index Checker starts providing value by checking that the accesses are not out of bounds.
Some types within the application might need to be annotated `@UnshrinkableRef` to accept both kinds of collections.

### Advanced features

Also see [advanced features](mutable-index-checking-advanced.md).

### Alternate qualifier hierarchies

Here are alternative, unacceptable qualifier hierarchy designs.

In this hierarchy, any `@GrowOnly` can be cast to `@Shrinkable` and have `remove()` called on it:

```text
bottom <: @GrowOnly <: @Shrinkable
```

In this hierarchy, any `@Shrinkable` can be cast to `@GrowOnly`, then
an alias of it can be modified:

```text
bottom <: @Shrinkable <: @GrowOnly
```

// LocalWords:  toc toclevels myList indexable SideEffectsOnly BackedBy Doop
// LocalWords:  ChangesLength LengthOf LTELengthOf hardbreaks GrowOnly
// LocalWords:  UnshrinkableRef UncheckedShrinkable IndexFor TODO
