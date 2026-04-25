package dev.betterrtp;

import dev.betterrtp.command.RtpCommand;
import dev.betterrtp.command.RtpTabCompleter;
import dev.betterrtp.config.RtpConfig;
import dev.betterrtp.cooldown.CooldownManager;
import dev.betterrtp.gui.GuiClickListener;
import dev.betterrtp.teleport.LocationCache;
import dev.betterrtp.teleport.LocationFinder;
import dev.betterrtp.teleport.Teleporter;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Map;

import java.util.Objects;

public final class BetterRTP extends JavaPlugin {

    private RtpConfig pluginConfig;
    private CooldownManager cooldownManager;
    private LocationFinder locationFinder;
    private LocationCache locationCache;
    private Teleporter teleporter;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.pluginConfig     = new RtpConfig(getConfig());
        this.cooldownManager  = new CooldownManager();
        this.locationFinder   = new LocationFinder(pluginConfig);
        this.locationCache    = new LocationCache(this, pluginConfig, locationFinder);
        this.teleporter       = new Teleporter(this);

        locationCache.start();

        PluginCommand rtpCommand = Objects.requireNonNull(
                getCommand("rtp"), "'rtp'-Befehl fehlt in plugin.yml");
        rtpCommand.setExecutor(new RtpCommand(this));
        rtpCommand.setTabCompleter(new RtpTabCompleter());
        getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);

        unregisterNamespacedAlias("betterrtp:rtp");

        getLogger().info("BetterRTP wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (locationCache != null) {
            locationCache.stop();
        }
        getLogger().info("BetterRTP wurde deaktiviert.");
    }

    public void reload() {
        reloadConfig();
        this.pluginConfig   = new RtpConfig(getConfig());
        this.locationFinder = new LocationFinder(pluginConfig);
        this.locationCache.reload(pluginConfig, locationFinder);
        getLogger().info("BetterRTP Konfiguration neu geladen.");
    }


    public RtpConfig getPluginConfig()        { return pluginConfig; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public LocationFinder getLocationFinder() { return locationFinder; }
    public LocationCache getLocationCache()   { return locationCache; }
    public Teleporter getTeleporter()         { return teleporter; }

    private void unregisterNamespacedAlias(String alias) {
        try {
            SimpleCommandMap commandMap = (SimpleCommandMap) getServer().getCommandMap();
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            knownCommands.remove(alias);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            getLogger().warning("Konnte Alias '" + alias + "' nicht entfernen: " + e.getMessage());
        }
    }
}
