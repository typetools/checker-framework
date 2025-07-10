package org.checkerframework.afu.scenelib.el;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.objectweb.asm.TypePath;

/**
 * A TypePathEntry is a way to get from one node in a {@link TypePath} to another. One can treat
 * these as edges in a graph.
 *
 * <p>A TypePathEntry corresponds to a step in an ASM {@link TypePath}.
 *
 * <p>{@code List<TypePathEntry>} corresponds to an ASM {@link TypePath}. {@code
 * List<TypePathEntry>} also corresponds to the javac class {@code
 * com.sun.tools.javac.code.TypeAnnotationPosition}.
 *
 * <p>{@code TypePathEntry} is immutable.
 */
public class TypePathEntry {
  /**
   * The kind of TypePathEntry; that is, how to get from the previous node in a TypePath to this
   * one. One of TypePath.ARRAY_ELEMENT, TypePath.INNER_TYPE, TypePath.WILDCARD_BOUND,
   * TypePath.TYPE_ARGUMENT.
   *
   * <p>This corresponds to javac class {@code
   * com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry}.
   */
  public final int step;

  /**
   * If this represents a type argument (that is, step == TYPE_ARGUMENT), then the index for the
   * type argument. Otherwise, 0.
   */
  public final int argument;

  /** The canonical ARRAY_ELEMENT TypePathEntry for building TypePaths. */
  public static final @InternedDistinct TypePathEntry ARRAY_ELEMENT =
      new TypePathEntry(TypePath.ARRAY_ELEMENT, 0);

  /** The canonical INNER_TYPE TypePathEntry for building TypePaths. */
  public static final @InternedDistinct TypePathEntry INNER_TYPE =
      new TypePathEntry(TypePath.INNER_TYPE, 0);

  /** The canonical WILDCARD_BOUND TypePathEntry for building TypePaths. */
  public static final @InternedDistinct TypePathEntry WILDCARD_BOUND =
      new TypePathEntry(TypePath.WILDCARD_BOUND, 0);

  /**
   * Construct a new TypePathEntry.
   *
   * @param step the type of the TypePathEntry
   * @param argument index of the type argument or 0
   */
  private TypePathEntry(int step, int argument) {
    this.step = step;
    this.argument = argument;
  }

  /**
   * Create a TypePathEntry.
   *
   * @param step the type of the TypePathEntry
   * @param argument index of the type argument or 0
   * @return a TypePathEntry
   */
  public static TypePathEntry create(int step, int argument) {
    switch (step) {
      case TypePath.ARRAY_ELEMENT:
        assert argument == 0;
        return ARRAY_ELEMENT;
      case TypePath.INNER_TYPE:
        assert argument == 0;
        return INNER_TYPE;
      case TypePath.WILDCARD_BOUND:
        assert argument == 0;
        return WILDCARD_BOUND;
      case TypePath.TYPE_ARGUMENT:
        return new TypePathEntry(step, argument);
      default:
        throw new Error("Bad step " + step);
    }
  }

  /**
   * Returns whether this {@link TypePathEntry} equals {@code o}; a slightly faster variant of
   * {@link #equals(Object)} for when the argument is statically known to be another nonnull {@link
   * TypePathEntry}.
   *
   * @param o the {@code TypePathEntry} to compare to this
   * @return true if this equals {@code o}
   */
  public boolean equals(TypePathEntry o) {
    return step == o.step && argument == o.argument;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof TypePathEntry && equals((TypePathEntry) o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(step, argument);
  }

  /**
   * Converts a TypePathEntry to a String. The TypePathEntry is passed in as its component parts:
   * step and argument.
   *
   * @param step the kind of TypePathEntry
   * @param argument a type index if the step == TYPE_ARGUMENT, otherwise ignored
   * @return the String reresentaion of the TypePathEntry
   */
  public static String toString(int step, int argument) {
    switch (step) {
      case TypePath.ARRAY_ELEMENT:
        return "[";
      case TypePath.INNER_TYPE:
        return ".";
      case TypePath.WILDCARD_BOUND:
        return "*";
      case TypePath.TYPE_ARGUMENT:
        return String.valueOf(argument) + ";";
      default:
        throw new Error("Bad step " + step);
    }
  }

  @Override
  public String toString() {
    return "\"" + toString(step, argument) + "\"";
  }

  /**
   * Converts a type path represented by a list of integers to a {@link TypePath}.
   *
   * @param integerList the integer list in the form [step1, argument1, step2, argument2, ...] where
   *     step1 and argument1 are the step and argument of the first entry (or edge) of a type path.
   *     Each step is a {@link TypePath} constant; see {@link #step}.
   * @return the {@link TypePath} corresponding to {@code integerList}, or null if the argument is
   *     null
   */
  public static TypePath getTypePathFromBinary(List<Integer> integerList) {
    if (integerList == null) {
      return null;
    }
    StringBuilder stringBuilder = new StringBuilder();
    Iterator<Integer> iterator = integerList.iterator();
    while (iterator.hasNext()) {
      int step = iterator.next();
      if (!iterator.hasNext()) {
        throw new IllegalArgumentException("Odd number of elements: " + integerList);
      }
      int argument = iterator.next();
      stringBuilder.append(toString(step, argument));
    }
    return TypePath.fromString(stringBuilder.toString());
  }

  /**
   * Converts a type path represented by a list of Integers to a list of {@link TypePathEntry}
   * elements.
   *
   * @param integerList the Integer list in the form [step1, argument1, step2, argument2, ...] where
   *     step1 and argument1 are the step and argument of the first entry (or edge) of a type path.
   *     Each step is a {@link TypePath} constant; see {@link #step}.
   * @return the list of {@link TypePathEntry} elements corresponding to {@code integerList}, or
   *     null if the argument is null
   */
  public static List<TypePathEntry> getTypePathEntryListFromBinary(List<Integer> integerList) {
    if (integerList == null) {
      return null;
    }
    List<TypePathEntry> typePathEntryList = new ArrayList<>();
    Iterator<Integer> iterator = integerList.iterator();
    while (iterator.hasNext()) {
      int step = iterator.next();
      if (!iterator.hasNext()) {
        throw new IllegalArgumentException("Odd number of elements: " + integerList);
      }
      int argument = iterator.next();
      typePathEntryList.add(new TypePathEntry(step, argument));
    }
    return typePathEntryList;
  }

  /**
   * Converts a type path represented by a list of {@link TypePathEntry} to a {@link TypePath}.
   *
   * @param typePathEntryList the {@link TypePathEntry} list corresponding to the location of some
   *     type annotation
   * @return the {@link TypePath} corresponding to {@code typePathEntryList}, or null if the
   *     argument is null or empty
   */
  public static TypePath listToTypePath(List<TypePathEntry> typePathEntryList) {
    if (typePathEntryList == null || typePathEntryList.isEmpty()) {
      return null;
    }
    StringBuilder stringBuilder = new StringBuilder();
    for (TypePathEntry typePathEntry : typePathEntryList) {
      stringBuilder.append(toString(typePathEntry.step, typePathEntry.argument));
    }
    return TypePath.fromString(stringBuilder.toString());
  }

  /**
   * Converts a {@link TypePath} to a list of {@link TypePathEntry} elements.
   *
   * @param typePath the {@link TypePath} corresponding to the location of some type annotation
   * @return the list of {@link TypePathEntry} elements corresponding to {@code typePath}, or null
   *     if the argument is null
   */
  public static List<TypePathEntry> typePathToList(TypePath typePath) {
    if (typePath == null) {
      return null;
    }
    List<TypePathEntry> typePathEntryList = new ArrayList<>(typePath.getLength());
    for (int index = 0; index < typePath.getLength(); index++) {
      typePathEntryList.add(
          new TypePathEntry(typePath.getStep(index), typePath.getStepArgument(index)));
    }
    return typePathEntryList;
  }
}
