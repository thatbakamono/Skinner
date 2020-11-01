package me.kernelerror.skinner;

import com.comphenix.protocol.wrappers.*;
import me.kernelerror.skinner.command.SkinCommand;
import me.kernelerror.skinner.mineskin.MineskinClient;
import me.kernelerror.skinner.mineskin.data.Skin;
import me.kernelerror.skinner.packetwrapper.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Consumer;

public class SkinnerPlugin extends JavaPlugin implements SkinnerAPI {
    private final ModDetector modDetector = new ModDetector(this);
    private final MineskinClient mineskinClient = new MineskinClient();
    private final File skinsDirectory = new File(getDataFolder(), "skins/");

    private Method updateScaledHealthMethod;

    @Override
    public void onEnable() {
        modDetector.initialize();

        try {
            updateScaledHealthMethod = Player.class.getMethod("updateScaledHealth");
        } catch (NoSuchMethodException exception) {
            throw new RuntimeException("You are using an unsupported version", exception);
        }

        getCommand("skin").setExecutor(new SkinCommand(this));

        if (!skinsDirectory.exists()) {
            if (!skinsDirectory.mkdir()) {
                throw new RuntimeException("Couldn't create the directory " + skinsDirectory.getName());
            }
        }
    }

    @Override
    public void setSkin(Player player, String url, Runnable callback) throws IOException {
        uploadSkin(player, url, skin -> {
            WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
            gameProfile.getProperties().clear();
            gameProfile.getProperties().put("textures", new WrappedSignedProperty("textures", skin.data.texture.value, skin.data.texture.signature));

            ArrayList<PlayerInfoData> playerInfoData = new ArrayList<PlayerInfoData>() {
                {
                    new PlayerInfoData(gameProfile, 0, EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()), WrappedChatComponent.fromText(player.getDisplayName()));
                }
            };

            updateSkinForAllPlayers(player, gameProfile.getHandle(), playerInfoData);
            callback.run();
        });
    }

    private void updateSkinForAllPlayers(Player player, Object playerHandle, ArrayList<PlayerInfoData> playerInfoData) {
        for (Player observer : Bukkit.getOnlinePlayers()) {
            WrapperPlayServerPlayerInfo removePlayerPacket = new WrapperPlayServerPlayerInfo();
            removePlayerPacket.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            removePlayerPacket.setData(playerInfoData);

            WrapperPlayServerPlayerInfo addPlayerPacket = new WrapperPlayServerPlayerInfo();
            addPlayerPacket.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            addPlayerPacket.setData(playerInfoData);

            if (player.equals(observer)) {
                updateSkinForMyself(player, playerHandle, removePlayerPacket, addPlayerPacket);
            } else {
                updateSkinForOtherPlayer(player, observer, removePlayerPacket, addPlayerPacket);
            }
        }
    }

    private void updateSkinForMyself(Player player, Object playerHandle, WrapperPlayServerPlayerInfo removePlayerPacket, WrapperPlayServerPlayerInfo addPlayerPacket) {
        World playerWorld = player.getWorld();
        Location playerLocation = player.getLocation();

        WrapperPlayServerRespawn respawnPacket = new WrapperPlayServerRespawn();
        respawnPacket.setDimension(playerWorld.getEnvironment().getId());
        respawnPacket.setDifficulty(EnumWrappers.Difficulty.valueOf(playerWorld.getDifficulty().name()));
        respawnPacket.setLevelType(playerWorld.getWorldType());
        respawnPacket.setGamemode(EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()));

        WrapperPlayServerPosition positionPacket = new WrapperPlayServerPosition();
        positionPacket.setX(playerLocation.getX());
        positionPacket.setY(playerLocation.getY());
        positionPacket.setZ(playerLocation.getZ());
        positionPacket.setYaw(playerLocation.getYaw());
        positionPacket.setPitch(playerLocation.getPitch());
        positionPacket.setFlags(new HashSet<>());

        WrapperPlayServerHeldItemSlot heldItemSlotPacket = new WrapperPlayServerHeldItemSlot();
        heldItemSlotPacket.setSlot(player.getInventory().getHeldItemSlot());

        try {
            Class<?> handleClass = playerHandle.getClass();

            removePlayerPacket.sendPacket(player);
            addPlayerPacket.sendPacket(player);
            respawnPacket.sendPacket(player);
            handleClass.getMethod("updateAbilities").invoke(playerHandle);
            positionPacket.sendPacket(player);
            heldItemSlotPacket.sendPacket(player);
            updateScaledHealthMethod.invoke(player);
            player.updateInventory();
            handleClass.getMethod("triggerHealthUpdate").invoke(playerHandle);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new RuntimeException("You are using an unsupported version", exception);
        }

        if (player.isOp()) {
            player.setOp(false);
            player.setOp(true);
        }
    }

    private void updateSkinForOtherPlayer(Player player, Player observer, WrapperPlayServerPlayerInfo removePlayerPacket, WrapperPlayServerPlayerInfo addPlayerPacket) {
        Location playerLocation = player.getLocation();

        WrapperPlayServerEntityDestroy entityDestroyPacket = new WrapperPlayServerEntityDestroy();
        entityDestroyPacket.setEntityIds(new int[] { player.getEntityId() });

        WrapperPlayServerNamedEntitySpawn namedEntitySpawnPacket = new WrapperPlayServerNamedEntitySpawn();
        namedEntitySpawnPacket.setEntityID(player.getEntityId());
        namedEntitySpawnPacket.setPlayerUUID(player.getUniqueId());
        namedEntitySpawnPacket.setMetadata(WrappedDataWatcher.getEntityWatcher(player));
        namedEntitySpawnPacket.setPosition(playerLocation.toVector());
        namedEntitySpawnPacket.setYaw(playerLocation.getYaw());
        namedEntitySpawnPacket.setPitch(playerLocation.getPitch());

        removePlayerPacket.sendPacket(observer);
        addPlayerPacket.sendPacket(observer);
        entityDestroyPacket.sendPacket(observer);
        namedEntitySpawnPacket.sendPacket(observer);
    }

    private void uploadSkin(Player player, String url, Consumer<Skin> callback) throws IOException {
        URL skinUrl = new URL(url);
        File skinFile = new File(getDataFolder(), "skins/" + player.getUniqueId() + ".skin");

        try (InputStream inputStream = skinUrl.openStream()) {
            Files.copy(inputStream, skinFile.toPath());
        }

        mineskinClient.generateUpload(skinFile, skin -> {
            if (!skinFile.delete()) {
                throw new RuntimeException("Couldn't delete the file " + skinFile.getName());
            }
            callback.accept(skin);
        });
    }
}