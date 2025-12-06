package net.ditto.networking;

import net.minecraft.util.Identifier;
import net.ditto.Magia;

public class MagiaPackets {
    public static final Identifier SYNC_MAGIA = new Identifier(Magia.MOD_ID, "sync_magia");
    public static final Identifier TOGGLE_COMBAT = new Identifier(Magia.MOD_ID, "toggle_combat");
    public static final Identifier EQUIP_SKILL = new Identifier(Magia.MOD_ID, "equip_skill");
    public static final Identifier SYNC_COMBAT = new Identifier(Magia.MOD_ID, "sync_combat");
    public static final Identifier USE_SKILL = new Identifier(Magia.MOD_ID, "use_skill");
    public static final Identifier SYNC_UNLOCKED = new Identifier(Magia.MOD_ID, "sync_unlocked");

    // Added Missing Packet ID
    public static final Identifier SYNC_RACE = new Identifier(Magia.MOD_ID, "sync_race");
}