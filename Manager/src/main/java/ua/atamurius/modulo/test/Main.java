package ua.atamurius.modulo.test;

import ua.atamurius.modulo.fs.FileWatcher;
import ua.atamurius.modulo.manager.Module;
import ua.atamurius.modulo.manager.ModuleManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class Main {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        System.out.println("Starting...");

        ModuleManager manager = new ModuleManager();
        FileWatcher watcher = new FileWatcher(manager);
        watcher.watch(new File("modules"));

        Callable<String> c = manager.lookup(Callable.class, "ua.atamurius.modulo.module1.TestCallable");
        while (! Thread.interrupted()) {
            try {
                System.out.println(c.call());
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (Module m: manager.getModules()) {
                System.out.printf(" - %s: %s%n", m, m.getDependencies());
            }
            System.in.read();
            watcher.update();
        }
    }
}
