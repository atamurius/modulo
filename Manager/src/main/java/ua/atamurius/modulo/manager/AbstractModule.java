package ua.atamurius.modulo.manager;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Common module operations.
 */
public abstract class AbstractModule {

    public interface Loader {
        /**
         * Tries to load class, returns null if class cannot be loaded.
         */
        Class<?> lookup(String className);

        /**
         * Checks if class was loaded using this loader.
         */
        boolean isSourceOf(String className);
    }

    private final Collection<ModuleStateListener> listeners = new CopyOnWriteArraySet<>();

    public void addModuleListener(ModuleStateListener listener) {
        listeners.add(listener);
    }

    public void removeModuleListener(ModuleStateListener listener) {
        listeners.remove(listener);
    }

    protected void triggerStateChange(Module module) {
        for (ModuleStateListener listener: listeners) {
            listener.stateChanged(module);
        }
    }

    public abstract Loader getLoader();
}
