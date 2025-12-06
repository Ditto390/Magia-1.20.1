package net.ditto.util;

public interface IPlayerMagia {
    int getMagia();
    void setMagia(int amount);
    int getMaxMagia();
    // Helper to easily modify magia
    default void addMagia(int amount) {
        setMagia(Math.min(getMagia() + amount, getMaxMagia()));
    }
    default void removeMagia(int amount) {
        setMagia(Math.max(0, getMagia() - amount));
    }
}
