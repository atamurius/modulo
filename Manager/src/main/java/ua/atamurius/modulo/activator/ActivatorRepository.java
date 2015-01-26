package ua.atamurius.modulo.activator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository of activators.
 */
public class ActivatorRepository {

    private static final Logger log = LoggerFactory.getLogger(ActivatorRepository.class);

    private final Map<Class<?>,Activator> activators = new HashMap<>();

    public ActivatorRepository() {
        activators.put(ActivatorFor.class, new DefaultActivator(this));
    }

    public void register(Class<? extends Annotation> target, Activator activator) {
        activators.put(target, activator);
    }

    public void unregister(Class<? extends Annotation> target) {
        activators.remove(target);
    }

    public boolean activate(Class<?> type) {
        boolean isActivated = false;
        for (Annotation annotation : type.getAnnotations()) {
            Activator activator = activators.get(typeOf(annotation));
            log.debug("{}{} activated by {}%n", type, annotation, activator);
            if (activator != null) {
                isActivated |= activator.activate(type, annotation);
            }
        }
        return isActivated;
    }

    private Class<?> typeOf(Annotation annotation) {
        return annotation.getClass().getInterfaces()[0];
    }

    public void deactivate(Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            Activator activator = activators.get(typeOf(annotation));
            if (activator != null) {
                activator.deactivate(type, annotation);
            }
        }
    }
}
