package com.reposilite.plugin.api;

import com.reposilite.journalist.Journalist;
import com.reposilite.journalist.Logger;
import com.reposilite.plugin.ExtensionsManagement;
import org.jetbrains.annotations.Nullable;

public abstract class ReposilitePlugin implements Journalist {

    @SuppressWarnings("unused")
    ExtensionsManagement extensionsManagement;

    public abstract @Nullable Facade initialize();

    public void setExtensionsManagement(ExtensionsManagement extensionsManagement) {
        if (extensionsManagement == null) {
            throw new IllegalStateException("Plugin has been already initialized with ExtensionManagement");
        }
        this.extensionsManagement = extensionsManagement;
    }

    public ExtensionsManagement getExtensionsManagement() {
        return extensionsManagement;
    }

    public Logger getLogger() {
        return getExtensionsManagement().getLogger();
    }

}