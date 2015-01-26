package ua.atamurius.modulo.module2;

import ua.atamurius.modulo.activator.Activator;
import ua.atamurius.modulo.activator.ActivatorFor;

@ActivatorFor(Notify.class)
public class ActivatorTest implements Activator<Notify> {

    @Override
    public boolean activate(Class<?> type, Notify target) {
        System.out.printf(target.value(), type.getName());
        return false;
    }

    @Override
    public void deactivate(Class<?> type, Notify target) {
    }
}
