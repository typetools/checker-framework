# Ideas for advanced features

## 1. Conversion of unique references

If we know, by some means of alias tracking, that a reference to a collection is
unique and used only in one place,
then it may be safely converted both ways between `@GrowOnly` and Shrinkable.
This allows to "freeze" and "unfreeze" the collection for a while.
That may be quite a common thing, so it could enable checking more code.
On the other hand, satisfying the necessary uniqueness conditions might be difficult.

## 2. Qualifier hierarchy for shrink-only

The above conversion could make useful a qualifier hierarchy that works in the opposite direction:

```text
Bottom <: ShrinkOnly <: CannotAddTo
Bottom <: Growable <: CannotAddTo
```

Maybe not very useful in isolation, but common pairs of qualifiers from the two hierarchies can be:

```text
MutableLength = Growable + Shrinkable
ImmutableLength = GrowOnly + ShrinkOnly
ConstLength = CannotAddTo + CannotRemoveFrom
```

That could enable reasoning about collections that need to maintain fixed length.

## 3. Same-length collections

A common coding pattern is that one index variable is used for multiple
collections of the same length.
The reason why this emerges is that if there is only one collection, using an
index variable is not necessary, because the enhanced for loop can be used.
So this might be one of the next steps to consider.

If the collections are constructed with all the elements, then maintaining the
following annotations should suffice to show that `@IndexFor(listA)` is also
`@IndexFor(listB)`:

```text
listA: `@ShrinkOnly`
listB: `@AtLeastSameLen(listA)` + `@GrowOnly`
```

To support collections that are created by adding elements to all of them in a
loop, the SameLen qualifier would need an offset that would be flow-sensitive
and inferred.

## 4. Unified effect annotation

If it comes to defining method effect annotations, considering the above point
"Having more elements is fine":
The methods add, remove, and non-mutating methods, can all use one annotation
`@EnsuresRelativeMinLen(list, n)`, with the meaning "sizeOfListOnReturn >=
sizeOfListOnEntry + n".
Then we could have:

* add: `@EnsuresRelativeMinLen(list, 1)`
* get: `@EnsuresRelativeMinLen(list, 0)`
* remove: `@EnsuresRelativeMinLen(list, -1)`

An unrestricted method would -- implementation details aside -- be
`@EnsuresRelativeMinLen(list, -infinity)`.

## 5. Simple checking of effects

Checking method effects in general is hard, but a simple
implementation might be able to cover a good number of cases:

* For code executed in sequence, sum together the n in the EnsuresRelativeMinLen
  of all called methods that can mutate the collection.
* For code executed conditionally, change `@EnsuresRelativeMinLen(list, n)` to
  `@EnsuresRelativeMinLen(list, 0)` if n > 0.
* For code executed in a loop, additionally change `@EnsuresRelativeMinLen(list,
  n)` to `@EnsuresRelativeMinLen(list, -infinity)` if n < 0.
* Consider using value range information about the loop bound, when it is
  available.
