package dev.steyn.minichat;

import dev.steyn.minichat.api.MiniChat;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServerLoadEvent.LoadType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiniChatPlugin extends JavaPlugin implements Listener {


    private LuckPerms luckPerms;
    public final static MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    public final static LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final MiniChatManager registry = new MiniChatManager();
    private String metaKey;
    private String fallbackFormat;

    public String getMetaKey() {
        return metaKey;
    }

    public static MiniChatPlugin getInstance() {
        return JavaPlugin.getPlugin(MiniChatPlugin.class);
    }

    public String getFallbackFormat() {
        return fallbackFormat;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public MiniChatManager getRegistry() {
        return registry;
    }

    @Override
    public void onEnable() {
        ServicesManager manager = Bukkit.getServicesManager();
        this.luckPerms = Objects.requireNonNull(manager.getRegistration(LuckPerms.class))
            .getProvider();
        Bukkit.getPluginManager().registerEvents(new MiniChatRenderer(luckPerms, this), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        manager.register(MiniChat.class, registry, this, ServicePriority.High);
        registerDefaults();
        readConfig();
        getSLF4JLogger().info("MiniChat {} is enabled!", this.getDescription().getVersion());
    }

    private void readConfig() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        this.fallbackFormat = config.getString("minichat.format.fallback",
            "<gray><name>: <message></gray>");
        this.metaKey = config.getString("minichat.format.key", "minichat.format");
    }

    private void registerDefaults() {
        registry.addPlaceholder(this, "displayName",
            (Function<Player, Component>) Player::displayName);
        registry.addPlaceholder(this, "prefix", p -> getMeta(p, CachedMetaData::getPrefix));
        registry.addPlaceholder(this, "suffix", p -> getMeta(p, CachedMetaData::getSuffix));
        registry.addPlaceholder(this, "teamDisplayName", Entity::teamDisplayName);
        registry.addPlaceholder(this, "world", p -> Component.text(p.getWorld().getName()));
    }

    @Override
    public void onDisable() {
        getSLF4JLogger().info("MiniChat is disabled.");
    }

    private Component getMeta(Player who, Function<CachedMetaData, String> handler) {
        User user = luckPerms.getUserManager().getUser(who.getUniqueId());
        if (user != null) {
            String x = handler.apply(user.getCachedData().getMetaData());
            if (x != null) {
                if (x.contains("&")) {
                    return LEGACY.deserialize(x);
                } else {
                    return MINI_MESSAGE.deserialize(x);
                }
            }
        }
        return Component.empty();
    }

    @EventHandler
    public void onReload(ServerLoadEvent e) {
        if (e.getType() == LoadType.RELOAD) {
            getLog4JLogger().warn(
                "MiniChat does not support reloads. Please restart asap to avoid problems");
        }
    }

}
