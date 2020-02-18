package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.type.TypeMirror;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;

public class AClassWrapper {
    private AClass theClass;

    private final Map<String, AMethodWrapper> methods = new HashMap<>();
    private final Map<String, AFieldWrapper> fields = new HashMap<>();

    public AClassWrapper(AClass theClass) {
        this.theClass = theClass;
    }

    public AMethodWrapper vivifyMethod(String methodName) {
        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        } else {
            AMethodWrapper wrapper = new AMethodWrapper(theClass.methods.getVivify(methodName));
            methods.put(methodName, wrapper);
            return wrapper;
        }
    }

    public Map<String, AMethodWrapper> getMethods() {
        return ImmutableMap.copyOf(methods);
    }

    public AFieldWrapper vivifyField(String fieldName, TypeMirror type) {
        if (fields.containsKey(fieldName)) {
            return fields.get(fieldName);
        } else {
            AFieldWrapper wrapper =
                    new AFieldWrapper(theClass.fields.getVivify(fieldName), type.toString());
            fields.put(fieldName, wrapper);
            return wrapper;
        }
    }

    public Map<String, AFieldWrapper> getFields() {
        return ImmutableMap.copyOf(fields);
    }

    public Collection<? extends Annotation> getAnnotations() {
        return theClass.tlAnnotationsHere;
    }
}
