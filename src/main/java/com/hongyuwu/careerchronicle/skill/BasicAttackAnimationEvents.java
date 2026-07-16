package com.hongyuwu.careerchronicle.skill;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.network.FxDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段3-任务6-设计文档-普通攻击动作系统.md §三: server-side trigger for basic (non-skill) attack
 * animations. {@code Player.attack(Entity)} is shared client/server code (confirmed by
 * decompiling {@code Player.java:1122} -- it's called from both {@code LocalPlayer} for client
 * prediction and {@code ServerPlayer} authoritatively), so {@code AttackEntityEvent} fires on
 * both sides; only the server dispatch is real (client-side firing is prediction noise we must
 * not double-dispatch on).
 *
 * <p><b>引擎审计修复 任务B / A7 (表现引擎全面审计报告_2026-07-15.md A7):</b> two additions on top
 * of the original design --
 * <ul>
 *   <li>{@code priority = EventPriority.LOWEST} with the default {@code receiveCanceled = false}:
 *       any earlier-priority handler (a claims/protection mod cancelling the attack) means this
 *       handler is never invoked at all, so a cancelled attack no longer dispatches an animation
 *       the actual hit never happens for.</li>
 *   <li>a per-player minimum resend interval, so rapid left-click spam doesn't broadcast a fresh
 *       fx packet (and hard-cut the animation back to frame 0) faster than the animation itself
 *       can be seen -- {@link CustomAnimationPlayer}-equivalent debounce, done here on the server
 *       since that's where the dispatch decision is made.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CareerChronicleMod.MOD_ID)
public final class BasicAttackAnimationEvents {

    /** Minimum ticks between two dispatched basic-attack fx packets for the same player,
     * regardless of weapon category -- deliberately not per-animation-duration (which would need
     * a lookup this server-side class has no reason to own) but a single conservative floor that
     * comfortably covers every shipped attack_*.json's hit frame (阶段3-任务6-设计文档 §五: shortest
     * is the dagger's 8-tick thrust) without needing to track per-category timing here. */
    private static final long MIN_RESEND_INTERVAL_TICKS = 8L;

    private static final Map<UUID, Long> LAST_DISPATCH_GAME_TIME = new ConcurrentHashMap<>();

    private BasicAttackAnimationEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ResourceLocation animId = AttackAnimationClassifier.classifyItem(mainHand);
        if (animId == null) {
            // Unmatched item (pickaxe/unarmed/etc.) -- no new behavior, vanilla swing stands.
            return;
        }
        if (!allowDispatch(serverPlayer.getUUID(), player.level().getGameTime())) {
            return;
        }
        FxDispatcher.dispatchBasicAttack(serverPlayer, animId);
    }

    /** Package-private pure function (UUID/gameTime in, boolean out, plus the side effect of
     * recording the accepted time) so the debounce math itself is unit-testable without a real
     * {@code ServerPlayer}/{@code Level} -- same split rationale as {@code AnimFxOp.applyAnimation}. */
    static boolean allowDispatch(UUID playerId, long currentGameTime) {
        Long last = LAST_DISPATCH_GAME_TIME.get(playerId);
        if (last != null && currentGameTime - last < MIN_RESEND_INTERVAL_TICKS) {
            return false;
        }
        LAST_DISPATCH_GAME_TIME.put(playerId, currentGameTime);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_DISPATCH_GAME_TIME.remove(event.getEntity().getUUID());
    }

    /** 引擎审计修复 任务B: the debounce map is keyed by player UUID forever otherwise -- a player
     * dying doesn't log them out, so without this the map would only ever shrink on logout, not on
     * death-without-logout-then-never-reconnecting. Not a leak in practice (bounded by concurrent
     * player count), but cheap to also clear here since the event is already directly relevant. */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LAST_DISPATCH_GAME_TIME.remove(serverPlayer.getUUID());
        }
    }

    static void clearForTesting() {
        LAST_DISPATCH_GAME_TIME.clear();
    }
}
