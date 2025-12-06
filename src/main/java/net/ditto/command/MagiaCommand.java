package net.ditto.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.ditto.Magia;
import net.ditto.race.Race;
import net.ditto.skill.Skill;
import net.ditto.util.IPlayerCombat;
import net.ditto.util.IPlayerRace;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

public class MagiaCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("magia")
                    .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2
                    .then(CommandManager.literal("race")
                            .then(CommandManager.literal("set")
                                    // STRUCTURE CHANGED: /magia race set <RACE> [PLAYER]
                                    // This prevents the game from suggesting player names when you try to type a race.

                                    .then(CommandManager.argument("race", StringArgumentType.word())
                                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                                    Arrays.stream(Race.values()).map(Enum::name), builder))

                                            // Case 1: Set Self (No player arg provided)
                                            .executes(context -> setRace(context, context.getSource().getPlayer(), StringArgumentType.getString(context, "race")))

                                            // Case 2: Set Other Player
                                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                                    .executes(context -> setRace(context, EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "race")))
                                            )
                                    )
                            )
                    )
            );
        });
    }

    private static int setRace(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, String raceName) {
        try {
            Race race = Race.valueOf(raceName.toUpperCase());
            IPlayerRace playerRace = (IPlayerRace) player;
            IPlayerCombat playerCombat = (IPlayerCombat) player;

            // 1. Set the race
            playerRace.magia$setRace(race);

            // 2. CLEAR old unlocked skills so they don't persist
            playerCombat.clearUnlockedSkills();

            // 3. Unlock starting skills for the new race
            for (Skill s : race.getStartingSkills()) {
                playerCombat.unlockSkill(s);
                player.sendMessage(Text.literal("Unlocked skill: " + s.getName()).formatted(Formatting.GREEN), false);
            }

            // 4. Sync everything to client using the new helper
            Magia.syncAllToClient(player);

            context.getSource().sendFeedback(() -> Text.literal("Set race of " + player.getName().getString() + " to " + race.name()).formatted(Formatting.GOLD), true);
            return Command.SINGLE_SUCCESS;

        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("Invalid race: " + raceName));
            return 0;
        }
    }
}
