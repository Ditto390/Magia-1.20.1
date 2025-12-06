package net.ditto.util;

import net.ditto.skill.Skill;
import java.util.Set;

public interface IPlayerCombat {
    boolean isInCombatMode();
    void setCombatMode(boolean active);

    // 0 to 4 for example (5 slots)
    Skill getEquippedSkill(int slot);
    void setEquippedSkill(int slot, Skill skill);

    // --- New Unlocking System ---
    void unlockSkill(Skill skill);
    void clearUnlockedSkills(); // Added this method
    boolean isSkillUnlocked(Skill skill);
    Set<Skill> getUnlockedSkills();

    // --- Skill Usage Progression ---
    int getSkillUsage(Skill skill);
    void setSkillUsage(Skill skill, int count);
    default void incrementSkillUsage(Skill skill) {
        setSkillUsage(skill, getSkillUsage(skill) + 1);
    }
}