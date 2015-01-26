package ua.atamurius.modulo.activator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActivatorFor(ActivatorFor.class)
public class DefaultActivator implements Activator<ActivatorFor> {

    private static final Logger log = LoggerFactory.getLogger(DefaultActivator.class);

    private final ActivatorRepository activators;

    public DefaultActivator(ActivatorRepository activators) {
        this.activators = activators;
    }

    @Override
    public boolean activate(Class<?> type, ActivatorFor target) {
        try {
            activators.register(
                    target.value(),
                    newInstanceOf(type));
            return true;
        }
        catch (Exception e) {
            log.error("Activation of {} for {} failed: {}", type, target.value(), e);
            return false;
        }
    }

    // TODO find appropriate place
    private Activator newInstanceOf(Class<?> type) {
        try {
            return (Activator) type.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(type +" cannot be instantiated using default constructor");
        }
    }

    @Override
    public void deactivate(Class<?> type, ActivatorFor target) {
        activators.unregister(target.value());
    }
}
