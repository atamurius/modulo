package ua.atamurius.modulo.activator;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Activator target annotation.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface ActivatorFor {
    Class<? extends Annotation> value();
}
