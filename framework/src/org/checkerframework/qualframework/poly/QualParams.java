package org.checkerframework.qualframework.poly;

import java.util.*;

/** A map of qualifier parameters.  A <code>QualParams</code> object maps
 * qualifier parameter names to <code>Wildcard</code> objects.
 */
public class QualParams<Q> implements Map<String, Wildcard<Q>> {
    private Map<String, Wildcard<Q>> map;

    /** Construct an empty qualifier parameter map. */
    public QualParams() {
        this.map = new HashMap<>();
    }

    /** Construct a map containing a single entry whose value is a qualifier
     * from the underlying hierarchy.
     */
    public QualParams(String name, Q qual) {
        this(name, new Wildcard<Q>(qual));
    }

    /** Construct a map containing a single entry.
     */
    public QualParams(String name, Wildcard<Q> qual) {
        this();
        this.map.put(name, qual);
    }

    public QualParams(Map<String, Wildcard<Q>> map) {
        this.map = new HashMap<>(map);
    }


    // More rawtype nonsense like in BaseQual.BaseLimit.  Once again, this is
    // safe because there are no values of type Q in TOP or BOTTOM.
    //
    // TODO: we may be able to get rid of BOTTOM and TOP entirely by building a
    // bottom/top QualParams<Q> from the declared qualifier parameters of the
    // particular class

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final QualParams BOTTOM = new QualParams("__BOTTOM__", (Wildcard)null);
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final QualParams TOP = new QualParams("__TOP__", (Wildcard)null);

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
    public QualParams<Q> substitute(String name, Wildcard<Q> substValue) {
        Map<String, Wildcard<Q>> subst = new HashMap<>();
        subst.put(name, substValue);
        return this.substituteAll(subst);
    }

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
        return new QualParams<Q>(newMap);
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
        if (o == null || !o.getClass().equals(QualParams.class))
            return false;

        @SuppressWarnings("unchecked")
        QualParams<Q> other = (QualParams<Q>)o;
        return this.map.equals(other.map);
    }

    @Override
    public Wildcard<Q> get(Object key) {
        return map.get(key);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
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

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public Collection<Wildcard<Q>> values() {
        // TODO: make immutable?
        return map.values();
    }

}
