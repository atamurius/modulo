package ua.atamurius.modulo.fs;

import ua.atamurius.modulo.manager.Module;
import ua.atamurius.modulo.manager.ModuleManager;

import java.io.File;
import java.util.*;

/**
 * File system watcher.
 * Modules are supposed to be located in some directory as JAR files.
 */
public class FileWatcher {

    private final Collection<File> roots = new HashSet<>();
    private final Map<File,Module> modules = new HashMap<>();
    private final Map<File,Long> lastModified = new HashMap<>();
    private final ModuleManager manager;

    public FileWatcher(ModuleManager manager) {
        this.manager = manager;
    }

    public void watch(File root) {
        if (! root.isDirectory())
            throw new IllegalArgumentException(root +" is not valid module directory");
        roots.add(root);
        collect(root.listFiles());
    }

    private void collect(File[] files) {
        for (File file: files) {
            System.out.println(file);
            if (file.isFile() && file.getName().toUpperCase().endsWith(".JAR")) {
                checkModule(file);
            }
            else if (file.isDirectory()) {
                collect(file.listFiles());
            }
        }
    }

    public void update() {
        // check for new and updated
        collect(roots.toArray(new File[roots.size()]));
        // check for removed
        for (File file: modules.keySet()) {
            if (! file.isFile()) {
                modules.get(file).unload();
                lastModified.put(file, 0L);
            }
        }
        // update invalidated
        for (Module module: modules.values()) {
            if (module.getState() == Module.State.INVALIDATED) {
                module.update();
            }
        }
    }

    private void checkModule(File file) {
        if (! modules.containsKey(file)) {
            modules.put(file, new Module(manager, file));
            lastModified.put(file, file.lastModified());
        }
        else {
            if (lastModified.get(file) != file.lastModified()) {
                lastModified.put(file, file.lastModified());
                modules.get(file).invalidate();
            }
        }
    }
}
