package ua.atamurius.modulo.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static ua.atamurius.modulo.manager.Module.State.FREEZED;
import static ua.atamurius.modulo.manager.Module.State.INVALIDATED;
import static ua.atamurius.modulo.manager.Module.State.UNLOADED;

/**
 * Module, represented by JAR with it's own class loader.
 * - Module is loaded as ACTIVE and collect dependency modules
 * - Module becomes INVALIDATED and invalidates dependent modules
 * - Module becomes UNLOADED and dependent modules became FREEZED
 * - Module is updated and clears dependencies
 */
public class Module {

    private static final Logger log = LoggerFactory.getLogger(Module.class);

    private final Collection<ModuleStateListener> listeners = new ArrayList<>();
    private final Collection<Module> dependencies = new ArrayList<>();

    public enum State { FREEZED, ACTIVE, INVALIDATED, UNLOADED }

    private State state = UNLOADED;
    private int version = 0;
    private File file;
    private ModuleManager parent;
    private Loader loader;


    public Module(ModuleManager manager, File file) {
        requireNonNull(file);
        this.file = file;
        this.parent = manager;
        update();
        manager.register(this);
    }

    public Collection<Module> getDependencies() {
        return unmodifiableCollection(dependencies);
    }

    private final ModuleStateListener dependencyListener = new ModuleStateListener() {
        @Override
        public void stateChanged(Module module) {
            switch (module.getState()) {
                case INVALIDATED:
                    if (state == FREEZED)
                        evaluateState();
                    else
                        invalidate();
                    break;
                case UNLOADED:
                    setState(FREEZED);
                    break;
            }
        }
    };

    private void evaluateState() {
        if (state == UNLOADED) {
            return; // nothing can be changed
        }
        Set<State> deps = new HashSet<>();
        for (Module dep: dependencies) {
            deps.add(dep.getState());
        }
        if (deps.contains(UNLOADED) || deps.contains(FREEZED)) {
            setState(FREEZED); // some dependencies are missing
        }
        else if (deps.contains(INVALIDATED) || state == FREEZED) {
            setState(INVALIDATED);
        }
        // TODO circular dependencies
    }

    public Loader getLoader() {
        return loader;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public void addDependency(Module module) {
        dependencies.add(module);
        module.addModuleListener(dependencyListener);
        log.debug("{} depends on {}", this, module);
    }

    public void unload() {
        setState(UNLOADED);
    }

    public String getName() {
        return file.getName().substring(0, file.getName().length() - 4);
    }

    public State getState() {
        return state;
    }

    public void addModuleListener(ModuleStateListener listener) {
        listeners.add(listener);
    }

    public void removeModuleListener(ModuleStateListener listener) {
        listeners.remove(listener);
    }

    private void setState(State state) {
        if (this.state != state) {
            this.state = state;
            log.debug("{} state changed", this);
            ModuleStateListener[] ls = listeners.toArray(new ModuleStateListener[listeners.size()]);
            for (ModuleStateListener listener: ls) {
                listener.stateChanged(this);
            }
        }
    }

    public void update() {
        if (! file.isFile()) {
            unload();
        }
        else {
            loader = new Loader();
            version++;
            clearDependencies();
            setState(State.ACTIVE);
        }
    }

    private void clearDependencies() {
        for (Module module: dependencies) {
            module.removeModuleListener(dependencyListener);
        }
        dependencies.clear();
    }

    public void invalidate() {
        if (getState() != FREEZED) {
            setState(INVALIDATED);
        }
    }

    @Override
    public String toString() {
        return format("%s:%d(%s)", getName(), version, state);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Module) && (((Module) obj).file.equals(file));
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    private URL getModuleURL() {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new InternalError("Cannot convert file to URL: "+ e);
        }
    }

    public class Loader extends URLClassLoader {
        private Loader() {
            super(new URL[] { getModuleURL() }, parent.getLoader(Module.this));
        }
        public Class<?> lookup(String className) {
            try {
                if (! isActive()) {
                    throw new IllegalStateException(Module.this.toString());
                }
                Class<?> cls = findLoadedClass(className);
                return (cls != null) ? cls : findClass(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        public boolean isSourceOf(String className) {
            return findLoadedClass(className) != null;
        }

        @Override
        public String toString() {
            return "Loader of "+ Module.this;
        }
    }
}








