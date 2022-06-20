package dev.steyn.minichat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class MiniChatRenderer implements ChatRenderer, Listener {

    private final LuckPerms luckPerms;
    private final MiniChatPlugin plugin;

    private static final PlainTextComponentSerializer TEXT = PlainTextComponentSerializer.plainText();

    public MiniChatRenderer(LuckPerms luckPerms, MiniChatPlugin plugin) {
        this.luckPerms = luckPerms;
        this.plugin = plugin;
    }

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName,
        @NotNull Component message, @NotNull Audience viewer) {
        CachedMetaData metaData = Objects.requireNonNull(
                luckPerms.getUserManager().getUser(source.getUniqueId())).getCachedData()
            .getMetaData();
        String format = metaData.getMetaValue(plugin.getMetaKey());
        if (format == null) {
            format = plugin.getFallbackFormat();
        }
        List<TagResolver.Single> templates = plugin.getRegistry().handle(source, message, viewer);
        TagResolver.Builder builder = TagResolver.builder();
        for (TagResolver.Single template : templates) {
            builder = builder
                    .resolver(template);
        }
        templates.add(Placeholder.parsed("name", source.getName()));
        String msg = TEXT.serialize(message);
        templates.add(Placeholder.parsed("msg", msg));
        templates.add(Placeholder.parsed("message",
            msg)); // fancy tags such as rainbow and gradient don't work unless plain text.
        return MiniChatPlugin.MINI_MESSAGE.deserialize(format, builder.build());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        event.renderer(this);
    }

}
