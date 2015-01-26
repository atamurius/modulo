package ua.atamurius.modulo.module1;

import ua.atamurius.modulo.module2.Notify;
import ua.atamurius.modulo.module2.Utility;

import java.util.concurrent.Callable;

@Notify("Check %s here!%n")
public class TestCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        return Utility.convert("Today's number is "+ (int) (10 * Math.random()));
    }
}
