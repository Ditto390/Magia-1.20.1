package net.ditto.skill;

public enum Skill {
    NONE("None", "No skill equipped", 0, 0xFFFFFF),
    FIREBALL("Fireball", "Launches a fiery projectile.", 5, 0xFF4500),
    DASH("Dash", "Quickly dash in movement direction.", 3, 0x00FFFF),
    HEAL("Heal", "Restores a small amount of health.", 12, 0x00FF00),
    SHIELD("Shield", "Blocks incoming damage for 2s.", 15, 0x0000FF);

    private final String name;
    private final String description;
    private final int cooldownSeconds;
    private final int color; // Placeholder for an icon

    Skill(String name, String description, int cooldownSeconds, int color) {
        this.name = name;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
        this.color = color;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public int getColor() { return color; }
}
