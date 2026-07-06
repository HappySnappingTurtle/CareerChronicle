package com.hongyuwu.careerchronicle.autotest;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class AutoTestCommand {
    private AutoTestCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("career-autotest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> {
                            AutoTestController controller = AutoTestController.getInstance();
                            if (controller.isRunning()) {
                                context.getSource().sendFailure(
                                        Component.literal("AutoTest is already running!").withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            controller.start();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("AutoTest started! Screenshots → run/autotest_screenshots/")
                                            .withStyle(ChatFormatting.GREEN), false);
                            return 1;
                        }))
                .then(Commands.literal("stop")
                        .executes(context -> {
                            AutoTestController controller = AutoTestController.getInstance();
                            if (!controller.isRunning()) {
                                context.getSource().sendFailure(
                                        Component.literal("AutoTest is not running.").withStyle(ChatFormatting.YELLOW));
                                return 0;
                            }
                            controller.stop();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("AutoTest stopped. Check autotest_results.log")
                                            .withStyle(ChatFormatting.GOLD), false);
                            return 1;
                        }))
                .then(Commands.literal("status")
                        .executes(context -> {
                            AutoTestController controller = AutoTestController.getInstance();
                            String status = controller.isRunning() ? "RUNNING"
                                    : controller.isFinished() ? "FINISHED" : "IDLE";
                            context.getSource().sendSuccess(() ->
                                    Component.literal("AutoTest status: " + status)
                                            .withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        })));
    }
}
