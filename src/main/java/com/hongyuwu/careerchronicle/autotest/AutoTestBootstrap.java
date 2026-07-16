package com.hongyuwu.careerchronicle.autotest;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.File;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID, value = Dist.CLIENT)
public final class AutoTestBootstrap {

    // Bug8 root cause (confirmed via javap byte-code audit of
    // Minecraft.setScreen() + real caller-stack/event-identity evidence from a
    // diagnostic build, see Bug8-设计文档 for the full writeup):
    //
    // Minecraft.setScreen(newScreen) reads `Screen current = this.screen`,
    // posts a ScreenEvent.Opening, and only AFTER that post() call returns does
    // it execute `this.screen = event.getNewScreen()` (the putfield is the
    // LAST thing the method does, using a local variable captured before the
    // event was posted -- nobody in our code called event.setNewScreen()).
    //
    // The previous version of this class called CreateWorldScreen.openFresh()
    // synchronously from inside the ScreenEvent.Opening handler for the very
    // first "null -> TitleScreen" transition, via mc.execute(runnable). Because
    // that handler already runs ON the render thread, BlockableEventLoop's
    // same-thread fast path (`if (isSameThread()) runnable.run();`) executes
    // the runnable INLINE instead of queueing it -- so openFresh() (and its
    // own, separate, complete setScreen(GenericDirtMessageScreen) /
    // setScreen(CreateWorldScreen) calls) ran REENTRANTLY, nested inside the
    // still-in-progress outer setScreen(TitleScreen) call. openFresh()'s own
    // nested calls correctly set `this.screen = CreateWorldScreen` and fully
    // complete. But once control unwound back to the OUTER setScreen(TitleScreen)
    // call, it resumed right where it left off and executed its own pending
    // `this.screen = TitleScreen` -- clobbering the CreateWorldScreen that had
    // just been set moments earlier. This was proven with a diagnostic build
    // that logged System.identityHashCode(event) at every event-bus priority
    // tier: the HIGHEST-priority log for the initial "null -> TitleScreen"
    // event and the LOWEST/MONITOR-priority logs for the *same event object*
    // (same identity hash) were ~3 seconds apart, with the entire
    // CreateWorldScreen creation sequence logged in between -- i.e. one single
    // post() call for one event, paused mid-dispatch while our handler
    // recursively drove a whole nested screen transition to completion.
    //
    // Fix: never call openFresh()/loadLevel() synchronously from inside a
    // ScreenEvent.Opening handler. Just record that TitleScreen was seen and
    // defer the actual work to the next ClientTickEvent, which is dispatched
    // from Minecraft.tick() -- a sibling call to the renderer, never nested
    // inside a setScreen() dispatch. By the time that tick fires, the outer
    // setScreen(TitleScreen) call has already fully returned (there is nothing
    // left on the stack for it to resume and clobber).
    private static final int POLL_BUDGET_TICKS = 20; // 1s safety net; openFresh() is synchronous once un-nested.

    private static boolean worldLoadTriggered = false;
    private static boolean pendingTitleScreenAction = false;
    private static boolean pendingWorldCreate = false;
    private static int createTickDelay = 0;
    private static boolean autoTestScheduled = false;
    private static int autoStartDelay = -1;
    private static boolean autoQuitScheduled = false;
    private static int autoQuitDelay = -1;

    private AutoTestBootstrap() {}

    public static boolean isEnabled() {
        return Boolean.getBoolean("careerchronicle.autotest");
    }

    public static void scheduleQuit() {
        autoQuitScheduled = true;
        autoQuitDelay = 40;
        CareerChronicleMod.LOGGER.info("[AutoTest] Auto-quit scheduled in 40 ticks");
    }

    /** Pure boundary check, extracted so the give-up decision has JUnit coverage (Bug8 P1-P4). */
    static boolean shouldGiveUp(int remainingTicks) {
        return remainingTicks <= 0;
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!isEnabled()) return;
        CareerChronicleMod.LOGGER.info("[AutoTest] ScreenEvent.Opening: {} -> {}",
                event.getCurrentScreen() == null ? "null" : event.getCurrentScreen().getClass().getSimpleName(),
                event.getNewScreen() == null ? "null" : event.getNewScreen().getClass().getSimpleName());
        if (worldLoadTriggered) return;
        if (!(event.getNewScreen() instanceof TitleScreen)) return;

        worldLoadTriggered = true;
        CareerChronicleMod.LOGGER.info("[AutoTest] TitleScreen detected, deferring autotest_world load/create to next tick");
        // Bug8 fix: do NOT touch CreateWorldScreen/loadLevel here -- this method
        // runs nested inside Minecraft.setScreen(TitleScreen)'s own event post().
        // Just flag it; onClientTick runs from a clean, non-nested call stack.
        pendingTitleScreenAction = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled()) return;
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (pendingTitleScreenAction) {
            pendingTitleScreenAction = false;
            triggerWorldLoadOrCreate(mc);
        }

        // Handle world creation delay
        if (pendingWorldCreate) {
            if (mc.screen instanceof CreateWorldScreen screen) {
                pendingWorldCreate = false;
                CareerChronicleMod.LOGGER.info("[AutoTest] CreateWorldScreen ready, configuring autotest_world...");
                configureAndCreateWorld(screen);
            } else if (!shouldGiveUp(createTickDelay)) {
                if (createTickDelay % 20 == 0) {
                    CareerChronicleMod.LOGGER.info("[AutoTest] Still waiting for CreateWorldScreen ({}s left), current screen: {}",
                            createTickDelay / 20, mc.screen == null ? "null" : mc.screen.getClass().getSimpleName());
                }
                createTickDelay--;
            } else {
                pendingWorldCreate = false;
                CareerChronicleMod.LOGGER.error("[AutoTest] Gave up waiting for CreateWorldScreen, still got: {} "
                                + "(unexpected -- openFresh() is supposed to be synchronous once called outside "
                                + "any ScreenEvent.Opening dispatch; this indicates a NEW, different problem)",
                        mc.screen == null ? "null" : mc.screen.getClass().getSimpleName());
            }
        }

        // Handle auto-start delay
        if (!autoTestScheduled && mc.player != null && mc.level != null) {
            autoTestScheduled = true;
            autoStartDelay = 100;
            CareerChronicleMod.LOGGER.info("[AutoTest] Player joined world, auto-start in 100 ticks (5 seconds)");
        }

        if (autoStartDelay > 0) {
            autoStartDelay--;
            if (autoStartDelay == 0) {
                CareerChronicleMod.LOGGER.info("[AutoTest] Starting AutoTestController...");
                AutoTestController.getInstance().start();
            }
        }

        // Handle auto-quit delay
        if (autoQuitScheduled && autoQuitDelay > 0) {
            autoQuitDelay--;
            if (autoQuitDelay == 0) {
                CareerChronicleMod.LOGGER.info("[AutoTest] Auto-quit triggered, stopping Minecraft...");
                mc.stop();
            }
        }
    }

    private static void triggerWorldLoadOrCreate(Minecraft mc) {
        CareerChronicleMod.LOGGER.info("[AutoTest] TitleScreen detected, attempting to load autotest_world");

        File savesDir = new File(mc.gameDirectory, "saves");
        File worldDir = new File(savesDir, "autotest_world");

        if (worldDir.exists() && worldDir.isDirectory()) {
            CareerChronicleMod.LOGGER.info("[AutoTest] Found existing autotest_world, loading...");
            mc.createWorldOpenFlows().loadLevel(mc.screen, "autotest_world");
        } else {
            CareerChronicleMod.LOGGER.info("[AutoTest] No existing autotest_world, creating new world...");
            CareerChronicleMod.LOGGER.info("[AutoTest] Calling CreateWorldScreen.openFresh()...");
            try {
                CreateWorldScreen.openFresh(mc, mc.screen);
                CareerChronicleMod.LOGGER.info("[AutoTest] openFresh() returned, mc.screen is now: {}",
                        mc.screen == null ? "null" : mc.screen.getClass().getSimpleName());
            } catch (Throwable t) {
                CareerChronicleMod.LOGGER.error("[AutoTest] openFresh() threw", t);
            }
            pendingWorldCreate = true;
            createTickDelay = POLL_BUDGET_TICKS;
        }
    }

    private static void configureAndCreateWorld(CreateWorldScreen screen) {
        WorldCreationUiState uiState = screen.getUiState();
        uiState.setName("autotest_world");
        uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        CareerChronicleMod.LOGGER.info("[AutoTest] World configured: name={}, gameMode={}",
                uiState.getName(), uiState.getGameMode());

        try {
            Method onCreate = CreateWorldScreen.class.getDeclaredMethod("onCreate");
            onCreate.setAccessible(true);
            onCreate.invoke(screen);
            CareerChronicleMod.LOGGER.info("[AutoTest] onCreate() invoked successfully");
        } catch (ReflectiveOperationException e) {
            CareerChronicleMod.LOGGER.error("[AutoTest] Failed to invoke onCreate() via reflection", e);
        }
    }
}
