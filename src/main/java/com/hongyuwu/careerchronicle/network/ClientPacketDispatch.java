package com.hongyuwu.careerchronicle.network;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.player.CareerDataSnapshot;
import java.lang.reflect.InvocationTargetException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

final class ClientPacketDispatch {
    private static final String CLIENT_HANDLERS =
            "com.hongyuwu.careerchronicle.client.ClientPacketHandlers";

    private ClientPacketDispatch() {
    }

    static void updateCareerSnapshot(CareerDataSnapshot snapshot) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> invoke("updateCareerSnapshot",
                new Class<?>[]{CareerDataSnapshot.class},
                snapshot));
    }

    static void openCareerScreen() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> invoke("openCareerScreen",
                new Class<?>[0]));
    }

    private static void invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> handlerClass = Class.forName(CLIENT_HANDLERS);
            handlerClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            CareerChronicleMod.LOGGER.error("Failed to dispatch Career Chronicle client packet handler {}.", methodName, exception);
        }
    }
}
