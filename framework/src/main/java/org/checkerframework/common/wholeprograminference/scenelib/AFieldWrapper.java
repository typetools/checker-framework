package org.checkerframework.common.wholeprograminference.scenelib;

import scenelib.annotations.el.AField;

public class AFieldWrapper {
    private final AField theField;

    private final String type;

    // Only used for method parameters.
    private String parameterName;

    public AFieldWrapper(AField theField, String type) {
        this.theField = theField;
        this.type = type;
    }

    public AField getTheField() {
        return theField;
    }

    public String getType() {
        return type;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
}
