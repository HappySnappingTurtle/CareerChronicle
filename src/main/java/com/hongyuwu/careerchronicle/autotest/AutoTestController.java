package com.hongyuwu.careerchronicle.autotest;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.client.ClientCareerData;
import com.hongyuwu.careerchronicle.player.CareerDataNbt;
import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class AutoTestController {
    private static AutoTestController instance;

    private final List<AutoTestStep> steps = new ArrayList<>();
    private int currentStep = -1;
    private int ticksInStep;
    private boolean running;
    private boolean finished;
    private final List<TestResult> results = new ArrayList<>();
    private File screenshotDir;
    private File logFile;
    private int screenshotCounter;

    private AutoTestController() {}

    public static AutoTestController getInstance() {
        if (instance == null) {
            instance = new AutoTestController();
        }
        return instance;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public void start() {
        if (running) return;

        Minecraft mc = Minecraft.getInstance();
        screenshotDir = new File(mc.gameDirectory, "autotest_screenshots");
        screenshotDir.mkdirs();
        logFile = new File(mc.gameDirectory, "autotest_results.log");
        screenshotCounter = 0;
        results.clear();
        currentStep = -1;
        ticksInStep = 0;
        finished = false;

        steps.clear();
        AutoTestScenarios.buildFullFlowSteps(steps);

        running = true;
        log("=== Career Chronicle AutoTest Started ===");
        log("Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log("Steps: " + steps.size());
        log("");
        CareerChronicleMod.LOGGER.info("[AutoTest] Started with {} steps", steps.size());
        advanceStep();
    }

    public void stop() {
        if (!running) return;
        running = false;
        finished = true;
        log("");
        log("=== AutoTest Stopped ===");
        writeSummary();
        CareerChronicleMod.LOGGER.info("[AutoTest] Stopped. {} results recorded.", results.size());
    }

    public void tick() {
        if (!running || finished) return;
        if (currentStep < 0 || currentStep >= steps.size()) {
            stop();
            return;
        }

        AutoTestStep step = steps.get(currentStep);
        ticksInStep++;

        if (ticksInStep == 1) {
            try {
                step.execute(this);
            } catch (Exception e) {
                recordResult(step.name(), false, "Exception: " + e.getMessage());
                CareerChronicleMod.LOGGER.error("[AutoTest] Step '{}' threw exception", step.name(), e);
                advanceStep();
                return;
            }
        }

        if (ticksInStep >= step.waitTicks()) {
            if (step.verify() != null) {
                try {
                    String verifyResult = step.verify().get();
                    if (verifyResult == null) {
                        recordResult(step.name(), true, "PASS");
                    } else {
                        recordResult(step.name(), false, verifyResult);
                    }
                } catch (Exception e) {
                    recordResult(step.name(), false, "Verify exception: " + e.getMessage());
                }
            }
            advanceStep();
        }
    }

    private void advanceStep() {
        currentStep++;
        ticksInStep = 0;
        if (currentStep >= steps.size()) {
            stop();
        }
    }

    public void recordResult(String name, boolean passed, String detail) {
        TestResult result = new TestResult(name, passed, detail);
        results.add(result);
        String prefix = passed ? "  PASS " : "  FAIL ";
        String line = prefix + name + " — " + detail;
        log(line);
        CareerChronicleMod.LOGGER.info("[AutoTest] {}", line);
    }

    public String takeScreenshot(String label) {
        screenshotCounter++;
        String filename = String.format("%03d_%s.png", screenshotCounter, sanitize(label));
        TestScreenshotCapture.capture(screenshotDir, filename);
        log("  [Screenshot] " + filename);
        return filename;
    }

    public CareerDataSnapshot snapshot() {
        return ClientCareerData.snapshot();
    }

    public File getScreenshotDir() {
        return screenshotDir;
    }

    public void sendChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (message.startsWith("/")) {
                mc.player.connection.sendCommand(message.substring(1));
            } else {
                mc.player.connection.sendChat(message);
            }
        }
    }

    private void log(String line) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            pw.println(line);
        } catch (IOException e) {
            CareerChronicleMod.LOGGER.warn("[AutoTest] Failed to write log: {}", e.getMessage());
        }
    }

    private void writeSummary() {
        long passed = results.stream().filter(TestResult::passed).count();
        long failed = results.stream().filter(r -> !r.passed()).count();
        log("");
        log("=== Summary ===");
        log("Total: " + results.size() + "  Passed: " + passed + "  Failed: " + failed);
        log("Screenshots saved to: " + screenshotDir.getAbsolutePath());
        log("Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (failed > 0) {
            log("");
            log("Failed tests:");
            for (TestResult r : results) {
                if (!r.passed()) {
                    log("  - " + r.name() + ": " + r.detail());
                }
            }
        }
    }

    private static String sanitize(String label) {
        return label.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }

    public record TestResult(String name, boolean passed, String detail) {}
}
