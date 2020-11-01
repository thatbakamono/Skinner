package me.kernelerror.skinner;

import org.bukkit.entity.Player;

import java.io.IOException;

public interface SkinnerAPI {
    void setSkin(Player player, String url, Runnable callback) throws IOException;
}