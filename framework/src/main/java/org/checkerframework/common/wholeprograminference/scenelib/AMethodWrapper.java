package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Name;
import scenelib.annotations.el.AMethod;

public class AMethodWrapper {
    private final AMethod theMethod;

    private String returnType = "java.lang.Object";

    private Map<Integer, AFieldWrapper> parameters = new HashMap<>();

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        // do not keep return types that start with a ?, because that's not valid Java
        if ("java.lang.Object".equals(this.returnType) && !returnType.startsWith("?")) {
            this.returnType = returnType;
        }
    }

    public AMethodWrapper(AMethod theMethod) {
        this.theMethod = theMethod;
    }

    public AMethod getAMethod() {
        return theMethod;
    }

    public AFieldWrapper vivifyParameter(int i, String type, Name simpleName) {
        if (parameters.containsKey(i)) {
            return parameters.get(i);
        } else {
            AFieldWrapper wrapper = new AFieldWrapper(theMethod.parameters.getVivify(i), type);
            parameters.put(i, wrapper);
            wrapper.setParameterName(simpleName.toString());
            return wrapper;
        }
    }

    public Map<Integer, AFieldWrapper> getParameters() {
        return ImmutableMap.copyOf(parameters);
    }
}
