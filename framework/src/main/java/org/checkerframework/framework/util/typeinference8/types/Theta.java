package org.checkerframework.framework.util.typeinference8.types;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.lang.model.type.TypeVariable;

/** A mapping from type variable to inference variable. */
public class Theta extends LinkedHashMap<TypeVariable, Variable> {
    private static final long serialVersionUID = 42L;

    private final List<Entry<TypeVariable, Variable>> entryList = new ArrayList<>();

    @Override
    public Variable put(TypeVariable key, Variable value) {
        assert !this.containsKey(key) || this.get(key).equals(value);
        if (!this.containsKey(key)) {
            entryList.add(new SimpleEntry<>(key, value));
        }
        return super.put(key, value);
    }
}
