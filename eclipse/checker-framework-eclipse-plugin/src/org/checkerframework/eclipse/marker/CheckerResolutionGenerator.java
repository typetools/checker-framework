package org.checkerframework.eclipse.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class CheckerResolutionGenerator implements IMarkerResolutionGenerator2 {

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        IMarkerResolution[] resolutions = new IMarkerResolution[] {new TypeResolution()};
        return resolutions;
    }

    @Override
    public boolean hasResolutions(IMarker marker) {
        return true;
    }
}
