package ua.atamurius.modulo.manager;

import ua.atamurius.modulo.service.ServiceProxy;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

/**
 * Module manager.
 */
public class ModuleManager extends AbstractModule {

    private Collection<Module> modules = new ArrayList<Module>();

    public void register(Module module) {
        modules.add(module);
        module.addModuleListener(new ModuleStateListener() {
            @Override
            public void stateChanged(Module module) {
                triggerStateChange(module);
            }
        });
        triggerStateChange(module);
    }

    public Collection<Module> getModules() {
        return unmodifiableCollection(modules);
    }

    private final ThreadLocal<Module> callerModule = new ThreadLocal<>();

    private final Dispatcher loader = new Dispatcher();

    @Override
    public Loader getLoader() {
        return loader;
    }

    protected ClassLoader getLoader(final Module module) {
        return new ClassLoader(loader) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                callerModule.set(module); // tracing which module made request
                try {
                    return super.loadClass(name, resolve);
                }
                finally {
                    callerModule.set(null);
                }
            }
        };
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }

    public Module findSource(String className) {
        for (Module module: modules) {
            if (module.getLoader().isSourceOf(className)) {
                return module;
            }
        }
        return null;
    }

    public <T> T lookup(Class<T> type, String impl) {
        return ServiceProxy.create(this, type, impl);
    }

    private class Dispatcher extends ClassLoader implements Loader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Module caller = callerModule.get();
            for (Module module: modules) {
                if (module.isActive() && module != caller) {
                    Class<?> cls = module.getLoader().lookup(name);
                    if (cls != null) {
                        if (caller != null) {
                            caller.addDependency(module, name);
                        }
                        return cls;
                    }
                }
            }
            throw new ClassNotFoundException(name);
        }

        @Override
        public String toString() {
            return "ModuleManager Loader #"+ hashCode();
        }

        @Override
        public Class<?> lookup(String name) {
            for (Module module: modules) {
                if (module.isActive()) {
                    Class<?> cls = module.getLoader().lookup(name);
                    if (cls != null) {
                        return cls;
                    }
                }
            }
            return null;
        }

        @Override
        public boolean isSourceOf(String className) {
            for (Module module: modules) {
                if (module.getLoader().isSourceOf(className))
                    return true;
            }
            return false;
        }
    };
}
