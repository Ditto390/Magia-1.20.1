package net.ditto.race;

import net.ditto.skill.Skill;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum Race {
    ARACHNE(Skill.DASH),
    ANGEL(Skill.HEAL),
    VAMPIRE(Skill.FIREBALL);

    private final List<Skill> startingSkills;
    private static final Random RANDOM = new Random();

    Race(Skill... startingSkills) {
        this.startingSkills = Arrays.asList(startingSkills);
    }

    public List<Skill> getStartingSkills() {
        return startingSkills;
    }

    /**
     * @return A random race from the list.
     */
    public static Race getRandom() {
        Race[] races = values();
        return races[RANDOM.nextInt(races.length)];
    }
}
