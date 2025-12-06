package net.ditto.mixin;

import net.ditto.race.Race;
import net.ditto.skill.Skill;
import net.ditto.util.IPlayerRace;
import net.ditto.util.IPlayerMagia;
import net.ditto.util.IPlayerCombat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IPlayerRace, IPlayerMagia, IPlayerCombat {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // --- Race Data ---
    @Unique private Race magia$race = null;
    @Override public Race magia$getRace() { return this.magia$race; }
    @Override public void magia$setRace(Race race) { this.magia$race = race; }

    // --- Magia Data ---
    @Unique private int magia = 10;
    @Unique private int maxMagia = 10;
    @Override public int getMagia() { return this.magia; }
    @Override public void setMagia(int amount) { this.magia = amount; }
    @Override public int getMaxMagia() { return this.maxMagia; }

    // --- Combat Data ---
    @Unique private boolean isCombatMode = false;
    @Unique private Skill[] equippedSkills = new Skill[]{Skill.NONE, Skill.NONE, Skill.NONE, Skill.NONE, Skill.NONE};

    @Unique private final Set<Skill> unlockedSkills = new HashSet<>();

    @Override
    public boolean isInCombatMode() {
        return this.isCombatMode;
    }

    @Override
    public void setCombatMode(boolean active) {
        this.isCombatMode = active;
    }

    @Override
    public Skill getEquippedSkill(int slot) {
        if (slot < 0 || slot >= equippedSkills.length) return Skill.NONE;
        return equippedSkills[slot];
    }

    @Override
    public void setEquippedSkill(int slot, Skill skill) {
        if (slot >= 0 && slot < equippedSkills.length) {
            this.equippedSkills[slot] = skill;
        }
    }

    @Override
    public void unlockSkill(Skill skill) {
        this.unlockedSkills.add(skill);
    }

    @Override
    public boolean isSkillUnlocked(Skill skill) {
        return this.unlockedSkills.contains(skill);
    }

    @Override
    public Set<Skill> getUnlockedSkills() {
        return this.unlockedSkills;
    }

    // =============================================================
    //                     RACE PASSIVES
    // =============================================================

    // 1. ARACHNE: Leap on Jump + Shift (BUFFED)
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    public void magia$arachneLeap(CallbackInfo ci) {
        if (this.magia$race == Race.ARACHNE && this.isSneaking()) {
            Vec3d look = this.getRotationVector();

            // Increased strength:
            // Forward multiplier: 1.2 -> 2.5
            // Upward kick: 0.8 -> 1.3
            this.setVelocity(this.getVelocity().add(look.x * 2.5, 1.3, look.z * 2.5));

            this.velocityModified = true;
            this.velocityDirty = true;
            ci.cancel();
        }
    }

    // 2. ARACHNE: No Fall Damage
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    public void magia$arachneNoFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (this.magia$race == Race.ARACHNE) {
            cir.setReturnValue(false);
        }
    }

    // 3. VAMPIRE: Lifesteal on Attack
    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;resetLastAttackedTicks()V"))
    public void magia$vampireLifesteal(Entity target, CallbackInfo ci) {
        if (this.magia$race == Race.VAMPIRE && target instanceof LivingEntity) {
            this.heal(1.0f);
        }
    }

    // 4. VAMPIRE: Burn in Sun
    @Inject(method = "tick", at = @At("TAIL"))
    public void magia$vampireBurn(CallbackInfo ci) {
        if (this.magia$race == Race.VAMPIRE && !this.getWorld().isClient && this.isAlive()) {
            PlayerEntity self = (PlayerEntity)(Object)this;
            if (self.isCreative() || self.isSpectator()) return;

            boolean isDay = this.getWorld().isDay();
            if (isDay && !this.getWorld().isRaining()) {
                float brightness = this.getBrightnessAtEyes();
                if (brightness > 0.5F && this.getWorld().isSkyVisible(this.getBlockPos())) {
                    this.setOnFireFor(8);
                }
            }
        }
    }

    // =============================================================

    // --- Persistence ---
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeCustomData(NbtCompound nbt, CallbackInfo ci) {
        // Race
        if (magia$race != null) nbt.putString("magia_race", magia$race.name());

        // Magia
        nbt.putInt("magia_amount", this.magia);
        nbt.putInt("magia_max", this.maxMagia);

        // Combat
        nbt.putBoolean("magia_combat_mode", this.isCombatMode);
        for(int i = 0; i < equippedSkills.length; i++) {
            nbt.putString("magia_skill_" + i, equippedSkills[i].name());
        }

        // Unlocked Skills
        NbtList skillsList = new NbtList();
        for (Skill s : unlockedSkills) {
            skillsList.add(NbtString.of(s.name()));
        }
        nbt.put("magia_unlocked_skills", skillsList);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readCustomData(NbtCompound nbt, CallbackInfo ci) {
        // Race
        if (nbt.contains("magia_race")) {
            try { this.magia$race = Race.valueOf(nbt.getString("magia_race")); } catch (Exception ignored) {}
        }

        // Magia
        if (nbt.contains("magia_amount")) this.magia = nbt.getInt("magia_amount");
        if (nbt.contains("magia_max")) this.maxMagia = nbt.getInt("magia_max");

        // Combat
        if (nbt.contains("magia_combat_mode")) this.isCombatMode = nbt.getBoolean("magia_combat_mode");
        for(int i = 0; i < equippedSkills.length; i++) {
            if(nbt.contains("magia_skill_" + i)) {
                try {
                    this.equippedSkills[i] = Skill.valueOf(nbt.getString("magia_skill_" + i));
                } catch (Exception e) {
                    this.equippedSkills[i] = Skill.NONE;
                }
            }
        }

        // Unlocked Skills
        if (nbt.contains("magia_unlocked_skills")) {
            this.unlockedSkills.clear();
            NbtList list = nbt.getList("magia_unlocked_skills", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                try {
                    this.unlockedSkills.add(Skill.valueOf(list.getString(i)));
                } catch (Exception ignored) {}
            }
        }
    }
}