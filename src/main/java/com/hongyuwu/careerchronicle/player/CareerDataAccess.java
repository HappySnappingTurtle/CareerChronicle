package com.hongyuwu.careerchronicle.player;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CareerDataAccess {
    private CareerDataAccess() {
    }

    public static Optional<ICareerData> get(Player player) {
        return player.getCapability(CareerDataCapability.CAREER_DATA).resolve();
    }

    public static void sync(ServerPlayer player) {
        get(player).ifPresent(data -> com.hongyuwu.careerchronicle.network.NetworkHandler.sendCareerSnapshot(
                player,
                data.snapshot()
        ));
    }
}
