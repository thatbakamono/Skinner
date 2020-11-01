package me.kernelerror.skinner.command;

import me.kernelerror.skinner.SkinnerAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class SkinCommand implements CommandExecutor {
    private final SkinnerAPI skinnerAPI;

    public SkinCommand(SkinnerAPI skinnerAPI) {
        this.skinnerAPI = skinnerAPI;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] arguments) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            if (arguments.length > 0) {
                try {
                    skinnerAPI.setSkin(player, arguments[0], () -> {});
                } catch (IOException exception) {
                    player.sendMessage("Something went wrong");
                }

                return false;
            }
        }

        return true;
    }
}