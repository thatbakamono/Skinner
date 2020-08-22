package me.kernelerror.skinner;

import org.bukkit.plugin.java.JavaPlugin;

public class SkinnerPlugin extends JavaPlugin {
    private final ModDetector modDetector = new ModDetector(this);

    @Override
    public void onEnable() {
        modDetector.initialize();
    }
}