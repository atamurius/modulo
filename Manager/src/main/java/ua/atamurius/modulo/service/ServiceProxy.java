package ua.atamurius.modulo.service;

import ua.atamurius.modulo.manager.Module;
import ua.atamurius.modulo.manager.ModuleManager;
import ua.atamurius.modulo.manager.ModuleStateListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ServiceProxy implements InvocationHandler {

    private final ModuleManager manager;
    private final String className;
    private Object instance;
    private Module source;

    private final ModuleStateListener listener = new ModuleStateListener() {
        @Override
        public void stateChanged(Module module) {
            if (! module.isActive()) {
                source.removeModuleListener(listener);
                instance = null;
                source = null;
            }
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> T create(ModuleManager manager, Class<T> type, String impl) {
        return (T) Proxy.newProxyInstance(
                (ClassLoader) manager.getLoader(),
                new Class<?>[]{ type },
                new ServiceProxy(manager, impl));
    }

    public ServiceProxy(ModuleManager manager, String className) {
        this.manager = manager;
        this.className = className;
    }

    private void ensureState() throws ReflectiveOperationException {
        if (instance == null) {
            try {
                this.instance = manager.loadClass(className).newInstance();
                if ((source = manager.findSource(className)) != null) {
                    source.addModuleListener(listener);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Class " + className + " is unavailable at the moment");
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ensureState();
        return method.invoke(instance, args);
    }
}
