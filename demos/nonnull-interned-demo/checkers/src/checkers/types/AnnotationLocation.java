package checkers.types;

import checkers.quals.*;

import java.util.*;

import javax.annotation.processing.*;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;

import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;

/**
 * Represents the location of an annotation within a type.  For example, consider
 *
 * <pre><code>@A HashMap&lt;@B String, @C List &lt;@D Object&gt;&gt;</code></pre>
 *
 * The <code>A</code> annotation is on the top-level type, the
 * <code>B</code> and <code>C</code> annotations are at the first level of
 * generic type arguments, and the <code>D</code> annotation is at the
 * second level of generic type arguments.
 * <p>
 *
 * AnnotationLocation is a wrapper for a list of integers; the <i>i</i>th
 * integer in the list holds the index (from zero) of a parameter at level
 * <i>i</i> (again, counting from zero) in the generic type.  Here are the
 * representations of the annotations given above:
 *
 * <table>
 * <tr><th> A <td>&nbsp;{@link AnnotationLocation#RAW} = []
 * <tr><th> B <td>&nbsp;[ 0 ]
 * <tr><th> C <td>&nbsp;[ 1 ]
 * <tr><th> D <td>&nbsp;[ 1, 0 ]
 * </table>
 *
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public final class AnnotationLocation {

    /** The location for an annotation on a raw type. */
    public static final AnnotationLocation RAW = new AnnotationLocation(
            new int[0]);

    /** The integer list specifying the location (see above). */
    private final List<@NonNull Integer> location;

    /**
     * Returns an annotation location for the given array. This method will
     * return {@link AnnotationLocation#RAW} for arrays of length zero.
     *
     * @return the {@link AnnotationLocation} for the given array
     */
    public static AnnotationLocation fromArray(int[] location) {
        if (Arrays.equals(location, new int[0]))
            return RAW;
        else return new AnnotationLocation(location);
    }

    /**
     * Returns an annotation location for the given list. This method will
     * return {@link AnnotationLocation#RAW} for lists of size zero.
     *
     * @return the {@link AnnotationLocation} for the given list
     */
    public static AnnotationLocation fromList(List<Integer> location) {
        if (location.size() == 0)
            return RAW;
        else return new AnnotationLocation(location);
    }

    /**
     * Creates a new instance of {@link AnnotationLocation}.
     *
     * @param location
     *            the integer list specifying the location (see above)
     */
    private AnnotationLocation(int[] location) {

        ArrayList<Integer> lst = new ArrayList<Integer>();
        if (location != null) {
            for (int i : location)
                lst.add(i);
        }

        this.location = Collections.<@NonNull Integer>unmodifiableList(lst);
    }

    /**
     * Creates a new instance of {@link AnnotationLocation}.
     *
     * @param location
     *            the integer list specifying the location (see above)
     */
    private AnnotationLocation(List<Integer> location) {
        this.location = Collections.<@NonNull Integer>unmodifiableList(new ArrayList<Integer>(location));
    }

    /**
     * Returns the location as a list of indices in the generic type.
     *
     * @return the integer list specifying the location (see above)
     */
    public List<@NonNull Integer> asList() {
        return location;
    }

    /**
     * @return true iff this falls within the subtree rooted at {@code root}
     */
    public boolean isSubtreeOf(AnnotationLocation root) {
        List<Integer> rootList = root.asList();
        if (rootList.size() > this.asList().size())
            return false;
        
        return rootList.equals(location.subList(0, rootList.size()));        
    }
    
    @Override
    public boolean equals(Object o) {
        return (o instanceof AnnotationLocation &&
                ((AnnotationLocation) o).location.equals(location));
    }

    @Override
    public int hashCode() {
        return location.hashCode() + 37;
    }

    @Override
    public String toString() {
        return location.toString();
    }

    /**
     * Return the type parameter within type that is specified by this.
     **/
    public @Nullable TypeMirror getTypeFrom(TypeMirror type) {
        TypeMirror current = type;
        // Walk down the generic type according to the integer list,
        // locating the parameter that this AnnotationLocation specifies.
        for (int index : location) {
            if (! (current instanceof DeclaredType)) {
//                throw new RuntimeException("current: " + current.getKind());
                return null;
            }
            DeclaredType declared = (DeclaredType) current;
            if (declared.getTypeArguments().size() <= index) {
                // Why doesn't this throw an exception? -MDE
                //System.err.println("INVALID: " + declared);
                return null;
            }
            @Nullable TypeMirror t = declared.getTypeArguments().get(index);
            assert t != null; /*nninvariant*/
            current = t;
        }
        return current;
    }
    
    public @Nullable Tree getTypeFrom(Tree typeTree) {
        Tree current = typeTree;
        
        for (int index : location) {
            if (current.getKind() != Tree.Kind.PARAMETERIZED_TYPE)
                throw new RuntimeException("current: " + current.getKind());
            ParameterizedTypeTree pt = (ParameterizedTypeTree) current;
            if (pt.getTypeArguments().size() <= index)
                return null;
             @Nullable Tree t = pt.getTypeArguments().get(index);
             assert t != null; /*nninvariant*/
             current = t;
        }
        return current;
    }

    /**
     * A utility method that returns a new {@code AnnotationData} as if it is rooted at the given newRoot.
     * This requires that the location of data is a subroot of newRoot.
     *
     * @param data  the AnnotationData to be copied
     * @param newRoot   the new {@code AnnotationData} root
     * @param env the current processing environment
     * @return a new {@code AnnotationData} of the same type and values as data with the new location
     */
     // TODO: Seriously consider moving this to a utilities class
    public static @Nullable AnnotationData asSubOf(AnnotationData data, AnnotationLocation newRoot,
            ProcessingEnvironment env) {
        if (!data.getLocation().isSubtreeOf(newRoot))
            return null;
        
        List<Integer> oldLocationList = data.getLocation().asList();
        List<Integer> newLocationList = data.getLocation().asList().subList(newRoot.asList().size(), oldLocationList.size());
        AnnotationLocation newLocation = AnnotationLocation.fromList(newLocationList);
        return new SyntheticAnnotation(data.getType(), newLocation, data.getValues(), env);
    }
}
