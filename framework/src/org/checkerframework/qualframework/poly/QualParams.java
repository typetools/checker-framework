package org.checkerframework.qualframework.poly;

import java.util.*;

/** A map of qualifier parameters.  A <code>QualParams</code> object maps
 * qualifier parameter names to <code>Wildcard</code> objects.
 */
public class QualParams<Q> implements Map<String, Wildcard<Q>> {
    private Map<String, Wildcard<Q>> map;
    private PolyQual<Q> primary;

    /** Construct an empty qualifier parameter map. */
    public QualParams() {
        this.map = new HashMap<>();
    }

    /** Construct a map containing a single entry whose value is a qualifier
     * from the underlying hierarchy.
     */
    public QualParams(String name, Q qual, PolyQual<Q> primary) {
        this(name, new Wildcard<Q>(qual), primary);
    }

    /** Construct a map containing a single entry.
     */
    public QualParams(String name, Wildcard<Q> qual, PolyQual<Q> primary) {
        this();
        this.map.put(name, qual);
        this.primary = primary;
    }

    public QualParams(Map<String, Wildcard<Q>> map, PolyQual<Q> primary) {
        this.map = new HashMap<>(map);
        this.primary = primary;
    }

    public QualParams(PolyQual<Q> primary) {
        this.primary = primary;
        this.map = new HashMap<>();
    }

    // More rawtype nonsense like in BaseQual.BaseLimit.  Once again, this is
    // safe because there are no values of type Q in TOP or BOTTOM.
    //
    // TODO: we may be able to get rid of BOTTOM and TOP entirely by building a
    // bottom/top QualParams<Q> from the declared qualifier parameters of the
    // particular class

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final QualParams BOTTOM = new QualParams("__BOTTOM__", (Wildcard)null, null);
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final QualParams TOP = new QualParams("__TOP__", (Wildcard)null, null);

    @SuppressWarnings("unchecked")
    public static <Q> QualParams<Q> getBottom() {
        return BOTTOM;
    }

    @SuppressWarnings("unchecked")
    public static <Q> QualParams<Q> getTop() {
        return TOP;
    }


    /** Apply capture conversion to each value in this map.
     */
    /*
    public QualParams<Q> capture() {
        if (this == QualParams.<Q>getTop() || this == QualParams.<Q>getBottom())
            return this;

        Map<String, Wildcard<Q>> newMap = new HashMap<>();
        for (String k : this.map.keySet()) {
            Wildcard<Q> newValue = this.map.get(k).capture();
            newMap.put(k, newValue);
        }
        return new QualParams<Q>(newMap);
    }
    */

    /** Apply a substitution to each value in this map.
     */
//    public QualParams<Q> substitute(String name, Wildcard<Q> substValue) {
//        Map<String, Wildcard<Q>> subst = new HashMap<>();
//        subst.put(name, substValue);
//        return this.substituteAll(subst);
//    }

    /** Apply a set of substitutions to each value in this map.
     */
    public QualParams<Q> substituteAll(Map<String, Wildcard<Q>> substs) {
        if (this == QualParams.<Q>getTop() || this == QualParams.<Q>getBottom())
            return this;

        Map<String, Wildcard<Q>> newMap = new HashMap<>();
        for (String k : this.map.keySet()) {
            Wildcard<Q> newValue = this.map.get(k).substitute(substs);
            newMap.put(k, newValue);
        }

        // Apply any substitutes for primary annotation @Vars
        Map<String, PolyQual<Q>> qualSubst = new HashMap<>();
        for (String k : substs.keySet()) {
            qualSubst.put(k, substs.get(k).getUpperBound());
        }
        PolyQual<Q> newPrimary = primary == null? null : primary.substitute(qualSubst);

        return new QualParams<Q>(newMap, newPrimary);
    }


    // The remaining functions implement the java.util.Map interface.

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Set<Map.Entry<String, Wildcard<Q>>> entrySet() {
        // TODO: make immutable?
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @SuppressWarnings("unchecked")
        QualParams<Q> that = (QualParams<Q>) o;

        if (map != null ? !map.equals(that.map) : that.map != null) return false;
        if (primary != null ? !primary.equals(that.primary) : that.primary != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = map != null ? map.hashCode() : 0;
        result = 31 * result + (primary != null ? primary.hashCode() : 0);
        return result;
    }

    @Override
    public Wildcard<Q> get(Object key) {
        return map.get(key);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        // TODO: make immutable?
        return map.keySet();
    }

    @Override
    public Wildcard<Q> put(String key, Wildcard<Q> value) {
        // TODO: make immutable?
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Wildcard<Q>> m) {
        // TODO: make immutable?
        map.putAll(m);
    }

    @Override
    public Wildcard<Q> remove(Object key) {
        // TODO: make immutable?
        return map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    public QualParams<Q> clone() {
        QualParams<Q> clone = new QualParams<Q>();
        clone.putAll(this);
        clone.setPrimary(getPrimary());
        return clone;
    }

    @Override
    public String toString() {
        String result = "QualParams(";
        result += "primary=" + primary + ",";

        result += map.toString();
        result += ")";
        return result;
    }

    @Override
    public Collection<Wildcard<Q>> values() {
        // TODO: make immutable?
        return map.values();
    }

    public PolyQual<Q> getPrimary() {
        return primary;
    }

    public void setPrimary(PolyQual<Q> primary) {
        this.primary = primary;
    }
}
