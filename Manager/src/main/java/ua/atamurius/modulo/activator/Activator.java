package ua.atamurius.modulo.activator;

import java.lang.annotation.Annotation;

/**
 * Activator class.
 */
public interface Activator<T extends Annotation> {

    boolean activate(Class<?> type, T target);

    void deactivate(Class<?> type, T target);
}
