package me.kernelerror.skinner;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class ModDetector implements Listener, PluginMessageListener {
    private final JavaPlugin plugin;

    private HashMap<UUID, HashSet<String>> playerMods = new HashMap<>();

    public ModDetector(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Channel for ModList packets is FML|HS
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "FML|HS", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "FML|HS");
    }

    public HashSet<String> getModsOfPlayer(Player player) {
        if (playerMods.containsKey(player.getUniqueId())) {
            return (HashSet<String>) playerMods.get(player.getUniqueId()).clone();
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        // First byte is discriminator, we are interested in packets with the discriminator 2 because they're ModList packets
        if (bytes[0] == 2) {
            int modCount = bytes[1] & 127;
            int bytesRead = 1;

            if ((bytes[1] & 128) == 128) {
                modCount |= (bytes[2] & 127) << 7;
                bytesRead++;
            }

            // Read array of pairs id - version
            int currentIndex = bytesRead + 1;

            for (int i = 0; i < modCount; i++) {
                String id = readString(bytes, currentIndex);

                currentIndex += getLengthOfString(bytes, currentIndex);

                String version = readString(bytes, currentIndex);

                currentIndex += getLengthOfString(bytes, currentIndex);

                if (playerMods.containsKey(player.getUniqueId())) {
                    playerMods.get(player.getUniqueId()).add(id);
                } else {
                    HashSet<String> mods = new HashSet<>();
                    mods.add(id);
                    playerMods.put(player.getUniqueId(), mods);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendPluginMessage(plugin, "FML|HS", new byte[] { -2, 0 });
            player.sendPluginMessage(plugin, "FML|HS", new byte[] { 0, 2, 0, 0, 0, 0 });
            player.sendPluginMessage(plugin, "FML|HS", new byte[] { 2, 0, 0, 0, 0 });
        }, 20);
    }

    private int getLengthOfVarInt(byte[] bytes, int startIndex) {
        int bytesRead = 0, currentIndex = startIndex - 1;

        do {
            bytesRead++;
        } while ((bytes[currentIndex++] & 128) == 128);

        return bytesRead;
    }

    private int readVarInt(byte[] bytes, int startIndex) {
        int result = 0, bytesRead = 0, currentIndex = startIndex;

        do {
            result |= (bytes[currentIndex] & 127) << (bytesRead++ * 7);
        } while ((bytes[currentIndex++] & 128) == 128);

        return result;
    }

    private int getLengthOfString(byte[] bytes, int startIndex) {
        int lengthOfVarInt = getLengthOfVarInt(bytes, startIndex);
        int lengthOfString = readVarInt(bytes, startIndex);

        return lengthOfVarInt + lengthOfString;
    }

    private String readString(byte[] bytes, int startIndex) {
        int currentIndex = startIndex;
        int length = readVarInt(bytes, currentIndex);

        currentIndex += getLengthOfVarInt(bytes, currentIndex);

        return new String(bytes, currentIndex, length);
    }
}