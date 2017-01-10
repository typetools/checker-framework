package org.checkerframework.eclipse.natures;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.eclipse.builder.CheckerBuilder;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class CheckerBuildNature implements IProjectNature {
    public static final String NATURE_ID = "checkers.eclipse.buildnature";
    private IProject project;

    @Override
    public void configure() throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] buildSpec = desc.getBuildSpec();

        for (ICommand command : buildSpec) {
            if (CheckerBuilder.BUILDER_ID.equals(command.getBuilderName())) {
                // already registered the builder for this project
                return;
            }
        }

        ICommand[] newSpec = new ICommand[buildSpec.length + 1];
        System.arraycopy(buildSpec, 0, newSpec, 0, buildSpec.length);
        ICommand newBuilder = desc.newCommand();
        newBuilder.setBuilderName(CheckerBuilder.BUILDER_ID);
        newSpec[newSpec.length - 1] = newBuilder;

        desc.setBuildSpec(newSpec);
        project.setDescription(desc, null);
    }

    @Override
    public void deconfigure() throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] buildSpec = desc.getBuildSpec();
        List<ICommand> newSpec = new ArrayList<ICommand>();

        for (ICommand command : buildSpec) {
            if (!CheckerBuilder.BUILDER_ID.equals(command.getBuilderName())) {
                newSpec.add(command);
            }
        }

        desc.setBuildSpec(newSpec.toArray(new ICommand[] {}));
        project.setDescription(desc, null);
    }

    @Override
    public IProject getProject() {
        return this.project;
    }

    @Override
    public void setProject(IProject project) {
        this.project = project;
    }
}
