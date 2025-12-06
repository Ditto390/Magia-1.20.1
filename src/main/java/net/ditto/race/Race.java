package net.ditto.race;

import java.util.Random;

public enum Race {
    ARACHNE,
    ANGEL,
    VAMPIRE;

    private static final Random RANDOM = new Random();

    /**
     * @return A random race from the list.
     */
    public static Race getRandom() {
        Race[] races = values();
        return races[RANDOM.nextInt(races.length)];
    }
}
