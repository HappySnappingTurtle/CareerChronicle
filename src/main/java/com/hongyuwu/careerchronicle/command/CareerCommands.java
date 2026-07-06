package com.hongyuwu.careerchronicle.command;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.career.CareerProgressionService;
import com.hongyuwu.careerchronicle.data.CareerRegistry;
import com.hongyuwu.careerchronicle.data.RegistrySnapshot;
import com.hongyuwu.careerchronicle.player.CareerDataAccess;
import com.hongyuwu.careerchronicle.player.ICareerData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class CareerCommands {
    private CareerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("career")
                .then(Commands.literal("debug")
                        .executes(context -> debug(context.getSource())))
                .then(Commands.literal("registry")
                        .executes(context -> registry(context.getSource())))
                .then(Commands.literal("set-race")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("race", StringArgumentType.string())
                                .executes(context -> setRace(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "race")
                                ))))
                .then(Commands.literal("add-class")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("class", StringArgumentType.string())
                                .executes(context -> addClass(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "class")
                                ))))
                .then(Commands.literal("add-xp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> addXp(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount")
                                ))))
                .then(Commands.literal("test")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> CareerTestCommand.runFullTest(context.getSource())))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> resetPlayer(context.getSource()))));
    }

    private static int resetPlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("careerchronicle.command.player_only"));
            return 0;
        }
        CareerDataAccess.get(player).ifPresent(data -> {
            data.deserializePersistentData(new net.minecraft.nbt.CompoundTag());
            data.getRuntimeState().clear();
        });
        CareerDataAccess.sync(player);
        source.sendSuccess(() -> Component.literal("Career data reset.").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int debug(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("careerchronicle.command.player_only"));
            return 0;
        }
        CareerDataAccess.get(player).ifPresentOrElse(
                data -> source.sendSuccess(() -> formatDebug(data), false),
                () -> source.sendFailure(Component.translatable("careerchronicle.command.no_data"))
        );
        return 1;
    }

    private static int setRace(CommandSourceStack source, String raceId) {
        return mutate(source, data -> data.setRace(id(raceId)));
    }

    private static int registry(CommandSourceStack source) {
        RegistrySnapshot snapshot = CareerRegistry.snapshot();
        source.sendSuccess(() -> Component.literal("Career Chronicle registry ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal("v=" + snapshot.version()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" races=" + snapshot.races().size()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" classes=" + snapshot.classes().size()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" skills=" + snapshot.skills().size()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" fusions=" + snapshot.fusions().size()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" hidden=" + snapshot.hiddenUnlocks().size()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" xp_sources=" + snapshot.xpSources().size()).withStyle(ChatFormatting.GRAY)), false);
        return 1;
    }

    private static int addClass(CommandSourceStack source, String classId) {
        return mutate(source, data -> data.addClassHistory(id(classId)));
    }

    private static int addXp(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("careerchronicle.command.player_only"));
            return 0;
        }
        CareerProgressionService.awardCareerXpFromSource(player, CareerProgressionService.COMMAND_XP_SOURCE, amount);
        CareerDataAccess.get(player).ifPresent(data -> source.sendSuccess(() -> formatDebug(data), false));
        return 1;
    }

    private static int mutate(CommandSourceStack source, DataMutation mutation) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("careerchronicle.command.player_only"));
            return 0;
        }
        CareerDataAccess.get(player).ifPresentOrElse(data -> {
            mutation.apply(data);
            CareerDataAccess.sync(player);
            source.sendSuccess(() -> formatDebug(data), false);
        }, () -> source.sendFailure(Component.translatable("careerchronicle.command.no_data")));
        return 1;
    }

    private static ResourceLocation id(String value) {
        if (!value.contains(":")) {
            return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, value);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed != null) {
            return parsed;
        }
        return ResourceLocation.fromNamespaceAndPath(CareerChronicleMod.MOD_ID, value);
    }

    private static Component formatDebug(ICareerData data) {
        return Component.literal("Career Chronicle ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("race=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(data.getRace().toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" level=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Integer.toString(data.getCareerLevel())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" xp=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Integer.toString(data.getCareerXp())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" classes=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(data.getClassHistory().toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" skills=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(data.getUnlockedSkills().toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" hidden=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(data.getHiddenFlags().toString()).withStyle(ChatFormatting.WHITE));
    }

    @FunctionalInterface
    private interface DataMutation {
        void apply(ICareerData data);
    }
}
