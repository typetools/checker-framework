package org.checkerframework.common.wholeprograminference.scenelib;

import scenelib.annotations.el.AClass;

/**
 * A wrapper for the AClass class from scene-lib that carries additional name and type information
 * (referred to as "symbol information" elsewhere) that is useful during WPI.
 *
 * <p>This would be better as a subclass of AClass.
 */
public class AClassWrapper {

    /** The wrapped AClass object. */
    public final AClass theClass;

    /**
     * Wrap an AClass. Package-private, because it should only be called from ASceneWrapper.
     *
     * @param theClass the wrapped object
     */
    AClassWrapper(AClass theClass) {
        this.theClass = theClass;
    }

    @Override
    public String toString() {
        return "AClassWrapper for " + theClass.toString();
    }
}
