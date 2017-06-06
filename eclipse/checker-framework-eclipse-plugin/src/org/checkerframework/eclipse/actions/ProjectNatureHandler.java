package org.checkerframework.eclipse.actions;

import org.checkerframework.eclipse.natures.CheckerBuildNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

public abstract class ProjectNatureHandler extends CheckerHandler {
    protected boolean hasNature(String[] natures) {
        for (String nature : natures) {
            if (CheckerBuildNature.NATURE_ID.equals(nature)) {
                return true;
            }
        }

        return false;
    }

    protected void removeNature(IProject project, IProjectDescription desc, String[] natures)
            throws CoreException {
        int skipIndex = 0;
        String[] newNatures = new String[natures.length - 1];

        for (int i = 0; i < natures.length; i++) {
            if (CheckerBuildNature.NATURE_ID.equals(natures[i])) {
                skipIndex = i;
            }
        }

        System.arraycopy(natures, 0, newNatures, 0, skipIndex);
        System.arraycopy(
                natures, skipIndex + 1, newNatures, skipIndex, newNatures.length - skipIndex);

        desc.setNatureIds(newNatures);
        project.setDescription(desc, null);
    }

    protected void setNature(IProject project, IProjectDescription desc, String[] natures)
            throws CoreException {

        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[newNatures.length - 1] = CheckerBuildNature.NATURE_ID;

        desc.setNatureIds(newNatures);

        project.setDescription(desc, null);
    }
}
