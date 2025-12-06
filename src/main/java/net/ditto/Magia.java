package net.ditto;

import net.ditto.networking.MagiaPackets;
import net.ditto.race.Race;
import net.ditto.skill.Skill;
import net.ditto.util.IPlayerCombat;
import net.ditto.util.IPlayerMagia;
import net.ditto.util.IPlayerRace;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Magia implements ModInitializer {
    public static final String MOD_ID = "magia";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Magia Mod...");

        // --- NETWORKING RECEIVERS ---

        // 1. Toggle Combat Mode
        ServerPlayNetworking.registerGlobalReceiver(MagiaPackets.TOGGLE_COMBAT, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                IPlayerCombat combatPlayer = (IPlayerCombat) player;
                boolean newState = !combatPlayer.isInCombatMode();
                combatPlayer.setCombatMode(newState);
                syncCombatToClient(player);
                player.sendMessage(Text.literal(newState ? "Combat Mode: ON" : "Combat Mode: OFF").formatted(newState ? Formatting.RED : Formatting.GREEN), true);
            });
        });

        // 2. Equip Skill
        ServerPlayNetworking.registerGlobalReceiver(MagiaPackets.EQUIP_SKILL, (server, player, handler, buf, responseSender) -> {
            int slot = buf.readInt();
            String skillName = buf.readString();
            server.execute(() -> {
                try {
                    Skill skill = Skill.valueOf(skillName);
                    ((IPlayerCombat) player).setEquippedSkill(slot, skill);
                    syncCombatToClient(player);
                } catch (Exception e) {
                    LOGGER.error("Failed to equip skill: " + skillName);
                }
            });
        });

        // 3. Use Skill (New)
        ServerPlayNetworking.registerGlobalReceiver(MagiaPackets.USE_SKILL, (server, player, handler, buf, responseSender) -> {
            int slot = buf.readInt();
            server.execute(() -> {
                IPlayerCombat combatPlayer = (IPlayerCombat) player;
                Skill skill = combatPlayer.getEquippedSkill(slot);

                if (skill != Skill.NONE) {
                    // Logic for specific skills
                    switch (skill) {
                        case DASH -> {
                            // Push player in direction they are looking
                            Vec3d look = player.getRotationVector();
                            // Multiply for speed (e.g. 1.5 blocks/tick force)
                            player.setVelocity(look.x * 1.5, look.y * 1.5, look.z * 1.5);
                            player.velocityModified = true; // Important to sync to client

                            // Play sound
                            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.0f, 1.5f);

                            player.sendMessage(Text.literal("Dash!").formatted(Formatting.AQUA), true);
                        }
                        case FIREBALL -> {
                            Vec3d look = player.getRotationVector();
                            // Create fireball (SmallFireballEntity doesn't destroy terrain as much as large ones)
                            // Arguments: World, Shooter, AccelX, AccelY, AccelZ
                            SmallFireballEntity fireball = new SmallFireballEntity(player.getWorld(), player, look.x, look.y, look.z);

                            // Set position to eye height so it doesn't spawn in feet
                            fireball.setPosition(player.getX(), player.getEyeY(), player.getZ());

                            // Spawn
                            player.getWorld().spawnEntity(fireball);

                            // Sound
                            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);

                            player.sendMessage(Text.literal("Fireball!").formatted(Formatting.GOLD), true);
                        }
                        case HEAL -> {
                            player.heal(4.0f); // Heals 2 hearts
                            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        }
                        default -> {
                            player.sendMessage(Text.literal("Skill implemented but no logic defined yet: " + skill.getName()), true);
                        }
                    }

                    // Optional: Deduct Magia cost here in the future
                }
            });
        });


        // --- EVENTS ---

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            IPlayerRace playerRace = (IPlayerRace) handler.player;

            if (playerRace.magia$getRace() == null) {
                Race newRace = Race.getRandom();
                playerRace.magia$setRace(newRace);
                handler.player.sendMessage(
                        Text.literal("You have been reborn as a: ")
                                .append(Text.literal(newRace.name()).formatted(Formatting.GOLD, Formatting.BOLD)),
                        false
                );
            }

            Magia.syncMagiaToClient(handler.player);
            Magia.syncCombatToClient(handler.player);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            ((IPlayerRace) newPlayer).magia$setRace(((IPlayerRace) oldPlayer).magia$getRace());
            ((IPlayerMagia) newPlayer).setMagia(((IPlayerMagia) oldPlayer).getMagia());

            IPlayerCombat oldC = (IPlayerCombat) oldPlayer;
            IPlayerCombat newC = (IPlayerCombat) newPlayer;
            newC.setCombatMode(oldC.isInCombatMode());
            for(int i=0; i<5; i++) {
                newC.setEquippedSkill(i, oldC.getEquippedSkill(i));
            }

            syncMagiaToClient(newPlayer);
            syncCombatToClient(newPlayer);
        });
    }

    public static void syncMagiaToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(((IPlayerMagia)player).getMagia());
        buf.writeInt(((IPlayerMagia)player).getMaxMagia());
        ServerPlayNetworking.send(player, MagiaPackets.SYNC_MAGIA, buf);
    }

    public static void syncCombatToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        IPlayerCombat pc = (IPlayerCombat) player;
        buf.writeBoolean(pc.isInCombatMode());
        for(int i=0; i<5; i++) {
            buf.writeString(pc.getEquippedSkill(i).name());
        }
        ServerPlayNetworking.send(player, MagiaPackets.SYNC_COMBAT, buf);
    }
}