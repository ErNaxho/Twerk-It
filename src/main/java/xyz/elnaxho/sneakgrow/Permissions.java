package xyz.elnaxho.sneakgrow;

/**
 * Central registry of every permission node used by the plugin.
 * Keeping these as constants (instead of scattering raw strings) makes it
 * trivial to add new commands/features without hunting through the codebase,
 * and keeps plugin.yml and the code in sync.
 */
public final class Permissions {

    private Permissions() {
    }

    // --- Commands ---------------------------------------------------------
    public static final String COMMAND_GROW = "sneakgrow.command.grow";
    public static final String COMMAND_GROW_SNEAK = "sneakgrow.command.grow.sneak";
    public static final String COMMAND_GROW_MOVE = "sneakgrow.command.grow.move";
    public static final String COMMAND_PLANT = "sneakgrow.command.plant";
    public static final String COMMAND_RELOAD = "sneakgrow.command.reload";

    // --- Features -----------------------------------------------------------
    public static final String FEATURE_AUTOGROW = "sneakgrow.feature.autogrow";
    public static final String FEATURE_AUTOGROW_MOVE = "sneakgrow.feature.autogrow.move";
    public static final String FEATURE_AUTOGROW_SNEAK = "sneakgrow.feature.autogrow.sneak";
    public static final String FEATURE_AUTOPLANT = "sneakgrow.feature.autoplant";
}
