package ua.atamurius.modulo.activator;

import ua.atamurius.modulo.manager.Module;
import ua.atamurius.modulo.manager.ModuleStateListener;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ua.atamurius.modulo.fs.ModuleContentEnumerator.contentsOf;

/**
 * Activate module classes.
 * TODO activate active modules with recently loaded activators to avoid load ordering
 */
public class ModuleActivator implements ModuleStateListener {

    private static final String CLASS = ".class";
    private final ActivatorRepository activators = new ActivatorRepository();

    private final Map<Module,Set<Class<?>>> activated = new HashMap<Module,Set<Class<?>>>() {
        @Override
        public Set<Class<?>> get(Object key) {
            if (! this.containsKey(key)) {
                this.put((Module) key, new HashSet<Class<?>>());
            }
            return super.get(key);
        }
    };

    public void activate(Module module) {
        for (String entry: contentsOf(module.getSource())) {
            if (entry.endsWith(CLASS)) {
                Class<?> cls = module.getLoader().lookup(toClassName(entry));
                if (cls != null && activators.activate(cls)) {
                    activated.get(module).add(cls);
                }
            }
        }
    }

    private String toClassName(String entry) {
        return entry.substring(0, entry.length() - CLASS.length()).replaceAll(File.separator, ".");
    }

    public void deactivate(Module module) {
        for (Class<?> cls : activated.get(module)) {
            activators.deactivate(cls);
        }
        activated.remove(module);
    }

    @Override
    public void stateChanged(Module module) {
        switch (module.getState()) {
            case ACTIVE:
                activate(module);
                break;
            default:
                deactivate(module);
        }
    }
}
