package dev.steyn.minichat;

import dev.steyn.minichat.api.MiniChat;
import dev.steyn.minichat.api.Placeholder;
import dev.steyn.minichat.api.Resolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class MiniChatManager implements MiniChat {

    private final Map<Plugin, List<Placeholder>> placeholderMap = new ConcurrentHashMap<>();
    private final Map<String, Placeholder> keys = new ConcurrentHashMap<>();


    @Override
    public void unregister(Placeholder placeholder) {
        List<Placeholder> placeholders = placeholderMap.get(placeholder.getPlugin());
        if (placeholders == null) {
            return;
        }
        placeholders.remove(placeholder);
    }

    @Override
    public void unregister(Plugin plugin) {
        List<Placeholder> placeholders = placeholderMap.get(plugin);
        if (placeholders == null) {
            return;
        }
        placeholderMap.remove(plugin);
        for (Placeholder placeholder : placeholders) {
            removeFor(placeholder.getKey(), placeholder);
            for (String alias : placeholder.getAliases()) {
                removeFor(alias, placeholder);
            }
        }
    }

    private void removeFor(String key, Placeholder who) {
        if (!keys.containsKey(key)) {
            return;
        }
        if (!keys.get(key).equals(who)) {
            return;
        }
        keys.remove(key);
    }


    @Override
    public Placeholder registerPlaceholder(Placeholder placeholder) {
        Plugin plugin = placeholder.getPlugin();
        List<Placeholder> placeholders;
        if (placeholderMap.containsKey(plugin)) {
            placeholders = placeholderMap.get(plugin);
        } else {
            placeholders = new ArrayList<>();
            placeholderMap.put(plugin, placeholders);
        }
        placeholders.add(placeholder);
        registerKey(placeholder.getKey(), placeholder);
        for (String alias : placeholder.getAliases()) {
            registerKey(alias, placeholder);
        }
        return placeholder;
    }

    private void registerKey(String key, Placeholder placeholder) {
        if (keys.containsKey(key)) {
            MiniChatPlugin.getInstance().getSLF4JLogger()
                .warn("Duplicate placeholder key {}. Plugin: {}", key,
                    placeholder.getPlugin().getName());
            return;
        }
        keys.put(key, placeholder);
    }

    @Override
    public Placeholder addPlaceholder(@NotNull Plugin plugin, @NotNull String key,
        @NotNull Function<Player, Component> resolver, String... aliases) {
        return addPlaceholder(plugin, key, (p, m, a) -> resolver.apply(p), aliases);
    }

    @Override
    public Placeholder addPlaceholder(@NotNull Plugin plugin, @NotNull String key,
        @NotNull BiFunction<Player, Audience, Component> resolver, String... aliases) {
        return addPlaceholder(plugin, key, (p, m, a) -> resolver.apply(p, a), aliases);
    }

    @Override
    public Placeholder addPlaceholder(@NotNull Plugin plugin, @NotNull String key,
        @NotNull Resolver resolver, String... aliases) {
        return registerPlaceholder(
            Placeholder.builder().plugin(plugin).key(key).resolver(resolver).aliases(aliases)
                .build());
    }


    public List<TagResolver.Single> handle(@NotNull Player who, @NotNull Component message,
                                           @NotNull Audience audience) {
        return this.keys.entrySet().stream().map(e -> net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(e.getKey(),
                e.getValue().getResolver().resolve(who, message, audience)))
            .collect(Collectors.toList());
    }

}
