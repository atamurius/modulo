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
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static ua.atamurius.modulo.manager.Module.State.FROZEN;
import static ua.atamurius.modulo.manager.Module.State.INVALIDATED;
import static ua.atamurius.modulo.manager.Module.State.UNLOADED;

/**
 * Module, represented by JAR with it's own class loader.
 * - Module is loaded as ACTIVE and collect dependency modules
 * - Module becomes INVALIDATED and invalidates dependent modules
 * - Module becomes UNLOADED and dependent modules became FROZEN
 * - Module is updated and clears dependencies
 */
public class Module extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Module.class);

    private final Map<Module,Set<String>> dependencies = new HashMap<>();

    private static final boolean COLLECT_CLASSES =
            ! "false".equalsIgnoreCase(System.getProperty(Module.class.getName() +".COLLECT_CLASSES"));

    public enum State { FROZEN, ACTIVE, INVALIDATED, UNLOADED }

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

    public File getSource() {
        return file;
    }

    public Collection<Module> getDependencies() {
        return unmodifiableCollection(dependencies.keySet());
    }

    public Collection<String> getDependencyClasses(Module module) {
        return dependencies.containsKey(module) ?
                unmodifiableCollection(dependencies.get(module)) :
                Collections.<String>emptySet();
    }

    private final ModuleStateListener dependencyListener = new ModuleStateListener() {
        @Override
        public void stateChanged(Module module) {
            switch (module.getState()) {
                case INVALIDATED:
                    if (state == FROZEN)
                        evaluateState();
                    else
                        invalidate();
                    break;
                case UNLOADED:
                    setState(FROZEN);
                    break;
            }
        }
    };

    private void evaluateState() {
        if (state == UNLOADED) {
            return; // nothing can be changed
        }
        Set<State> deps = new HashSet<>();
        for (Module dep: dependencies.keySet()) {
            deps.add(dep.getState());
        }
        if (deps.contains(UNLOADED) || deps.contains(FROZEN)) {
            setState(FROZEN); // some dependencies are missing
        }
        else if (deps.contains(INVALIDATED) || state == FROZEN) {
            setState(INVALIDATED);
        }
        // TODO circular dependencies
    }

    @Override
    public Loader getLoader() {
        return loader;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public void addDependency(Module module, String requestedClass) {
        if (! dependencies.containsKey(module)) {
            dependencies.put(module, new HashSet<String>());
            module.addModuleListener(dependencyListener);
        }
        if (COLLECT_CLASSES) {
            dependencies.get(module).add(requestedClass);
        }
        log.debug("{} depends on {} through {}", this, module, requestedClass);
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


    private void setState(State state) {
        if (this.state != state) {
            this.state = state;
            log.debug("{} state changed", this);
            triggerStateChange(this);
        }
    }

    public void update() {
        if (! file.isFile()) {
            unload();
        }
        else {
            loader = new ModuleLoader();
            version++;
            clearDependencies();
            setState(State.ACTIVE);
        }
    }

    private void clearDependencies() {
        for (Module module: dependencies.keySet()) {
            module.removeModuleListener(dependencyListener);
        }
        dependencies.clear();
    }

    public void invalidate() {
        if (getState() != FROZEN) {
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

    private class ModuleLoader extends URLClassLoader implements Loader {

        ModuleLoader() {
            super(new URL[] { getModuleURL() }, parent.getLoader(Module.this));
        }

        final int version = Module.this.version;

        @Override
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

        @Override
        public boolean isSourceOf(String className) {
            return findLoadedClass(className) != null;
        }

        @Override
        public String toString() {
            return format("%s@Loader:%d", getName(), version);
        }
    }
}








