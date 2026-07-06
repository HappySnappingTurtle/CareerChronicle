package com.hongyuwu.careerchronicle.autotest;

import java.util.function.Supplier;

public final class AutoTestStep {
    private final String name;
    private final int waitTicks;
    private final StepAction action;
    private final Supplier<String> verifier;

    private AutoTestStep(String name, int waitTicks, StepAction action, Supplier<String> verifier) {
        this.name = name;
        this.waitTicks = waitTicks;
        this.action = action;
        this.verifier = verifier;
    }

    public static AutoTestStep action(String name, int waitTicks, StepAction action) {
        return new AutoTestStep(name, waitTicks, action, null);
    }

    public static AutoTestStep verified(String name, int waitTicks, StepAction action, Supplier<String> verifier) {
        return new AutoTestStep(name, waitTicks, action, verifier);
    }

    public static AutoTestStep screenshot(String label) {
        return new AutoTestStep("Screenshot: " + label, 2, ctrl -> ctrl.takeScreenshot(label), null);
    }

    public static AutoTestStep wait(String label, int ticks) {
        return new AutoTestStep("Wait: " + label, ticks, ctrl -> {}, null);
    }

    public static AutoTestStep command(String cmd, int waitTicks) {
        return new AutoTestStep("Command: " + cmd, waitTicks, ctrl -> ctrl.sendChat("/" + cmd), null);
    }

    public String name() { return name; }
    public int waitTicks() { return waitTicks; }
    public Supplier<String> verify() { return verifier; }

    public void execute(AutoTestController controller) {
        action.run(controller);
    }

    @FunctionalInterface
    public interface StepAction {
        void run(AutoTestController controller);
    }
}
