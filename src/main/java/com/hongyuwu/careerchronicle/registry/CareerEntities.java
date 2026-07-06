package com.hongyuwu.careerchronicle.registry;

import com.hongyuwu.careerchronicle.CareerChronicleMod;
import com.hongyuwu.careerchronicle.world.entity.CareerProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CareerEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CareerChronicleMod.MOD_ID);

    public static final RegistryObject<EntityType<CareerProjectileEntity>> CAREER_PROJECTILE =
            ENTITY_TYPES.register("career_projectile", () -> EntityType.Builder
                    .<CareerProjectileEntity>of(CareerProjectileEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(4)
                    .updateInterval(2)
                    .build("career_projectile"));

    private CareerEntities() {
    }
}
