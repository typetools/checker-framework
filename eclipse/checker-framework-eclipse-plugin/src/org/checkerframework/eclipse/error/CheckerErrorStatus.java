package org.checkerframework.eclipse.error;

import org.checkerframework.eclipse.CheckerPlugin;
import org.eclipse.core.runtime.IStatus;

public class CheckerErrorStatus implements IStatus {
    private String message;

    public CheckerErrorStatus(String message) {
        this.message = message;
    }

    @Override
    public IStatus[] getChildren() {
        return new IStatus[] {};
    }

    @Override
    public int getCode() {
        return 0;
    }

    @Override
    public Throwable getException() {
        return null;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getPlugin() {
        return CheckerPlugin.PLUGIN_ID;
    }

    @Override
    public int getSeverity() {
        return IStatus.ERROR;
    }

    @Override
    public boolean isMultiStatus() {
        return false;
    }

    @Override
    public boolean isOK() {
        return false;
    }

    @Override
    public boolean matches(int severityMask) {
        return (severityMask & IStatus.ERROR) != 0;
    }
}
