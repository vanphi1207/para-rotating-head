package me.ihqqq.rotatingheads.command;

import me.ihqqq.rotatingheads.config.HeadRepository;
import me.ihqqq.rotatingheads.config.Language;
import me.ihqqq.rotatingheads.config.SettingsHolder;
import me.ihqqq.rotatingheads.model.HeadModels.Brightness;
import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.model.HeadModels.HeadOptions;
import me.ihqqq.rotatingheads.model.HeadModels.Hologram;
import me.ihqqq.rotatingheads.model.HeadModels.Interaction;
import me.ihqqq.rotatingheads.runtime.HeadRuntime;
import me.ihqqq.rotatingheads.util.HeadUtil;
import me.ihqqq.rotatingheads.util.MessageException;
import me.ihqqq.rotatingheads.util.TabUtil;
import me.ihqqq.rotatingheads.util.TextureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrhCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "rotatingheads.admin";
    private static final List<String> SUBCOMMANDS = List.of("reload", "list", "info", "near",
            "create", "clone", "edit", "teleport", "movehere", "delete", "benchmark");
    private static final List<String> EDIT_OPTIONS = List.of("texture", "item", "scale",
            "speed", "speedx", "speedz", "bobheight", "bobperiod", "brightness", "range",
            "interaction", "hologram", "offsety", "refresh", "provider", "link", "follow",
            "line");
    private static final List<String> LINE_ACTIONS = List.of("add", "set", "remove", "clear");
    private static final List<String> PROVIDERS = List.of("native", "fancyholograms");
    private static final List<String> BOOLEANS = List.of("true", "false");
    private static final int MAX_ITEM_SUGGESTIONS = 50;

    private final HeadRepository repository;
    private final HeadRuntime runtime;
    private final Map<String, Head> heads;
    private final SettingsHolder settings;
    private final Language language;

    public PrhCommand(HeadRepository repository, HeadRuntime runtime,
                      Map<String, Head> heads, SettingsHolder settings,
                      Language language) {
        this.repository = repository;
        this.runtime = runtime;
        this.heads = heads;
        this.settings = settings;
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(language.message("command.no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(language.message("command.usage"));
            return true;
        }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> reload(sender);
                case "list" -> list(sender);
                case "info" -> info(sender, args);
                case "near" -> near(sender, args);
                case "create" -> create(sender, args);
                case "clone" -> cloneHead(sender, args);
                case "edit" -> edit(sender, args);
                case "teleport" -> teleport(sender, args);
                case "movehere" -> move(sender, args);
                case "delete" -> delete(sender, args);
                case "benchmark" -> benchmark(sender, args);
                default -> sender.sendMessage(language.message("command.usage"));
            }
        } catch (MessageException exception) {
            sender.sendMessage(language.message(exception.key(), exception.replacements()));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(language.message("command.invalid", "reason",
                    String.valueOf(exception.getMessage())));
        }
        return true;
    }


    private void reload(CommandSender sender) {
        settings.reload();
        language.reload();
        runtime.reset();
        repository.load();
        heads.clear();
        heads.putAll(repository.all());
        runtime.load(heads);
        sender.sendMessage(language.message("command.reload"));
    }

    private void list(CommandSender sender) {
        if (heads.isEmpty()) {
            sender.sendMessage(language.message("command.no-heads"));
            return;
        }
        Component line = language.message("command.list-header", "count",
                String.valueOf(heads.size()));
        boolean first = true;
        for (String id : heads.keySet()) {
            if (!first) {
                line = line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
            }
            line = line.append(language.message("command.list-entry", "id", id));
            first = false;
        }
        sender.sendMessage(line);
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(language.message("command.info", "configured",
                    String.valueOf(heads.size()), "indexed",
                    String.valueOf(runtime.activeIds().size())));
            return;
        }
        Head head = requireHead(args[1]);
        HeadOptions options = head.options();
        Hologram hologram = head.hologram();
        Location location = head.location();
        sender.sendMessage(language.message("command.head-info",
                "id", head.id(),
                "world", location.getWorld() == null ? "?" : location.getWorld().getName(),
                "x", HeadUtil.format(location.getX()),
                "y", HeadUtil.format(location.getY()),
                "z", HeadUtil.format(location.getZ()),
                "item", options.item(),
                "scale", HeadUtil.format(options.scale()),
                "speedY", HeadUtil.format(options.speed()),
                "speedX", HeadUtil.format(options.speedX()),
                "speedZ", HeadUtil.format(options.speedZ()),
                "bob", HeadUtil.format(options.bobHeight()),
                "period", String.valueOf(options.bobPeriod()),
                "range", String.valueOf(head.displayRange()),
                "interaction", String.valueOf(head.interaction().enabled()),
                "hologram", String.valueOf(hologram.enabled()),
                "provider", hologram.provider(),
                "lines", String.valueOf(hologram.lines().size())));
        if (hologram.linked()) {
            sender.sendMessage(language.message(runtime.linkActive(head.id())
                            ? "command.link-linked" : "command.link-waiting",
                    "link", hologram.link(), "follow", String.valueOf(hologram.followHead())));
        }
    }

    private void near(CommandSender sender, String[] args) {
        Player player = player(sender);
        int radius = args.length > 1 ? HeadUtil.boundedInt(args[1], 1, 1000) : 32;
        Location origin = player.getLocation();
        List<Head> found = new ArrayList<>();
        for (Head head : heads.values()) {
            Location location = head.location();
            if (location.getWorld() != null && location.getWorld().equals(origin.getWorld())
                    && location.distance(origin) <= radius) {
                found.add(head);
            }
        }
        if (found.isEmpty()) {
            sender.sendMessage(language.message("command.near-none", "radius",
                    String.valueOf(radius)));
            return;
        }
        found.sort(Comparator.comparingDouble(head -> head.location().distanceSquared(origin)));
        sender.sendMessage(language.message("command.near-header", "count",
                String.valueOf(found.size()), "radius", String.valueOf(radius)));
        for (Head head : found) {
            sender.sendMessage(language.message("command.near-entry", "id", head.id(),
                    "distance", HeadUtil.format(Math.round(head.location().distance(origin)))));
        }
    }

    private void create(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        String id = HeadUtil.normalizeId(args[1]);
        if (heads.containsKey(id)) {
            sender.sendMessage(language.message("command.exists", "id", id));
            return;
        }
        String texture = args.length > 2
                ? TextureUtil.normalize(String.join(" ", Arrays.copyOfRange(args, 2, args.length)))
                : settings.get().defaultTexture();
        Head head = new Head(id, player.getLocation(), 64,
                new HeadOptions(texture, 2, 1, new Brightness(15, 15)),
                new Interaction(false, List.of(), List.of(), List.of()),
                new Hologram(false, 1, 0, List.of(), 1, false, true,
                        "default", new Brightness(15, 15)));
        store(head);
        sender.sendMessage(language.message("command.created", "id", id));
    }

    private void cloneHead(CommandSender sender, String[] args) {
        require(args, 3);
        Head source = requireHead(args[1]);
        String id = HeadUtil.normalizeId(args[2]);
        if (heads.containsKey(id)) {
            sender.sendMessage(language.message("command.exists", "id", id));
            return;
        }
        Head copy = source.withId(id);
        if (sender instanceof Player player) {
            copy = copy.at(player.getLocation());
        }
        store(copy);
        sender.sendMessage(language.message("command.cloned", "id", source.id(), "target", id));
    }

    private void edit(CommandSender sender, String[] args) {
        require(args, 4);
        Head old = requireHead(args[1]);
        String option = args[2].toLowerCase(Locale.ROOT);
        String value = args[3];
        HeadOptions options = old.options();
        Hologram hologram = old.hologram();
        Head updated = switch (option) {
            case "texture" -> old.withOptions(options.withTexture(TextureUtil.normalize(
                    String.join(" ", Arrays.copyOfRange(args, 3, args.length)))));
            case "item" -> old.withOptions(options.withItem(parseItem(value)));
            case "scale" -> old.withOptions(options.withScale(
                    HeadUtil.boundedDouble(value, 0.05, 20)));
            case "speed" -> old.withOptions(options.withSpeed(
                    HeadUtil.boundedDouble(value, -20, 20)));
            case "speedx" -> old.withOptions(options.withSpeedX(
                    HeadUtil.boundedDouble(value, -20, 20)));
            case "speedz" -> old.withOptions(options.withSpeedZ(
                    HeadUtil.boundedDouble(value, -20, 20)));
            case "bobheight" -> old.withOptions(options.withBobHeight(
                    HeadUtil.boundedDouble(value, 0, 5)));
            case "bobperiod" -> old.withOptions(options.withBobPeriod(
                    HeadUtil.boundedInt(value, 10, 1200)));
            case "brightness" -> {
                int[] parts = HeadUtil.parseBrightness(value);
                yield old.withOptions(options.withBrightness(
                        new Brightness(parts[0], parts[1])));
            }
            case "range" -> old.withDisplayRange(HeadUtil.boundedInt(value, 1, 256));
            case "interaction" -> old.withInteraction(
                    old.interaction().withEnabled(HeadUtil.parseBoolean(value)));
            case "hologram" -> old.withHologram(
                    hologram.withEnabled(HeadUtil.parseBoolean(value)));
            case "offsety" -> old.withHologram(
                    hologram.withOffsetY(HeadUtil.boundedDouble(value, -10, 10)));
            case "refresh" -> old.withHologram(
                    hologram.withRefreshTicks(HeadUtil.boundedInt(value, 0, 72000)));
            case "provider" -> old.withHologram(hologram.withProvider(parseProvider(value)));
            case "link" -> {
                String name = HeadUtil.linkName(value);
                Hologram linked = hologram.withLink(name);
                yield old.withHologram(name.isEmpty() ? linked
                        : linked.withProvider("fancyholograms"));
            }
            case "follow" -> old.withHologram(
                    hologram.withFollowHead(HeadUtil.parseBoolean(value)));
            case "line" -> old.withHologram(hologram.withLines(editLines(hologram, args)));
            default -> throw new MessageException("command.unknown-option", "option",
                    args[2], "options", String.join(", ", EDIT_OPTIONS));
        };
        store(updated);
        sender.sendMessage(language.message("command.updated", "id", updated.id()));
        if (option.equals("link") && updated.hologram().linked()) {
            if (!runtime.fancyAvailable()) {
                sender.sendMessage(language.message("command.fancy-missing"));
            }
            if (!updated.hologram().enabled()) {
                sender.sendMessage(language.message("command.link-hologram-disabled",
                        "id", updated.id()));
            }
        }
    }

    private List<String> editLines(Hologram hologram, String[] args) {
        String action = args[3].toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>(hologram.lines());
        switch (action) {
            case "add" -> {
                require(args, 5);
                lines.add(String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
            }
            case "set" -> {
                require(args, 6);
                lines.set(HeadUtil.lineIndex(args[4], lines.size()),
                        String.join(" ", Arrays.copyOfRange(args, 5, args.length)));
            }
            case "remove" -> {
                require(args, 5);
                lines.remove(HeadUtil.lineIndex(args[4], lines.size()));
            }
            case "clear" -> lines.clear();
            default -> throw new MessageException("command.unknown-option", "option",
                    args[3], "options", String.join(", ", LINE_ACTIONS));
        }
        return lines;
    }

    private String parseItem(String value) {
        Material material = Material.matchMaterial(value);
        if (material == null || !material.isItem() || material.isAir()) {
            throw new MessageException("validation.item", "value", value);
        }
        return material.name();
    }

    private String parseProvider(String value) {
        String provider = value.toLowerCase(Locale.ROOT);
        if (!PROVIDERS.contains(provider)) {
            throw new MessageException("validation.provider", "options",
                    String.join(", ", PROVIDERS));
        }
        return provider;
    }

    private void delete(CommandSender sender, String[] args) {
        require(args, 2);
        String id = args[1];
        if (heads.remove(id) == null) {
            throw new MessageException("command.unknown", "id", id);
        }
        runtime.remove(id);
        repository.remove(id);
        sender.sendMessage(language.message("command.deleted", "id", id));
    }

    private void teleport(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        player.teleport(requireHead(args[1]).location());
        sender.sendMessage(language.message("command.teleported", "id", args[1]));
    }

    private void move(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        Head updated = requireHead(args[1]).at(player.getLocation());
        store(updated);
        sender.sendMessage(language.message("command.moved", "id", args[1]));
    }

    private void benchmark(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        if ("clear".equalsIgnoreCase(args[1])) {
            runtime.clearBenchmarks();
            sender.sendMessage(language.message("command.benchmark-cleared"));
            return;
        }
        int count = args.length > 2 ? HeadUtil.boundedInt(args[2], 1, 500) : 100;
        runtime.spawnBenchmark(requireHead(args[1]), player.getLocation(), count);
        sender.sendMessage(language.message("command.benchmark-spawned", "count",
                String.valueOf(count)));
    }

    private void store(Head head) {
        heads.put(head.id(), head);
        repository.save(head);
        runtime.register(head);
    }

    private Player player(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new MessageException("command.player-only");
        }
        return player;
    }

    private Head requireHead(String id) {
        Head head = heads.get(id);
        if (head == null) {
            throw new MessageException("command.unknown", "id", id);
        }
        return head;
    }

    private void require(String[] args, int count) {
        if (args.length < count) {
            throw new MessageException("command.missing-argument", "usage", usageFor(args));
        }
    }

    private String usageFor(String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        return switch (sub) {
            case "create" -> "/prh create <id> [texture]";
            case "clone" -> "/prh clone <id> <new-id>";
            case "info" -> "/prh info [id]";
            case "near" -> "/prh near [radius]";
            case "edit" -> args.length >= 3 && "line".equalsIgnoreCase(args[2])
                    ? "/prh edit <id> line <add <text>|set <n> <text>|remove <n>|clear>"
                    : "/prh edit <id> <" + String.join("|", EDIT_OPTIONS) + "> <value>";
            case "teleport" -> "/prh teleport <id>";
            case "movehere" -> "/prh movehere <id>";
            case "delete" -> "/prh delete <id>";
            case "benchmark" -> "/prh benchmark <id> [count] | /prh benchmark clear";
            default -> "/prh <" + String.join("|", SUBCOMMANDS) + ">";
        };
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION) || args.length == 0) {
            return List.of();
        }
        String current = args[args.length - 1];
        if (args.length == 1) {
            return TabUtil.filter(SUBCOMMANDS, current);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "teleport", "movehere", "delete", "info", "clone" -> {
                return args.length == 2 ? TabUtil.filter(heads.keySet(), current) : List.of();
            }
            case "near" -> {
                return args.length == 2
                        ? TabUtil.filter(List.of("16", "32", "64", "128", "256"), current)
                        : List.of();
            }
            case "benchmark" -> {
                if (args.length == 2) {
                    List<String> options = new ArrayList<>(heads.keySet());
                    options.add("clear");
                    return TabUtil.filter(options, current);
                }
                if (args.length == 3 && !"clear".equalsIgnoreCase(args[1])) {
                    return TabUtil.filter(List.of("10", "50", "100", "250", "500"), current);
                }
                return List.of();
            }
            case "edit" -> {
                return completeEdit(args, current);
            }
            default -> {
                return List.of();
            }
        }
    }

    private List<String> completeEdit(String[] args, String current) {
        if (args.length == 2) {
            return TabUtil.filter(heads.keySet(), current);
        }
        if (args.length == 3) {
            return TabUtil.filter(EDIT_OPTIONS, current);
        }
        String option = args[2].toLowerCase(Locale.ROOT);
        if (option.equals("line")) {
            return completeLine(args, current);
        }
        if (args.length != 4) {
            return List.of();
        }
        Head head = heads.get(args[1]);
        return TabUtil.filter(editValues(head, option, current), current);
    }

    private List<String> completeLine(String[] args, String current) {
        if (args.length == 4) {
            return TabUtil.filter(LINE_ACTIONS, current);
        }
        Head head = heads.get(args[1]);
        String action = args[3].toLowerCase(Locale.ROOT);
        if (args.length == 5 && head != null
                && (action.equals("set") || action.equals("remove"))) {
            List<String> numbers = new ArrayList<>();
            for (int i = 1; i <= head.hologram().lines().size(); i++) {
                numbers.add(String.valueOf(i));
            }
            return TabUtil.filter(numbers, current);
        }
        return List.of();
    }

    private List<String> editValues(Head head, String option, String current) {
        return switch (option) {
            case "scale" -> withCurrent(List.of("0.5", "1", "1.5", "2", "3", "5"),
                    head == null ? null : HeadUtil.format(head.options().scale()));
            case "speed" -> withCurrent(List.of("-2", "-1", "0", "0.5", "1", "2", "5"),
                    head == null ? null : HeadUtil.format(head.options().speed()));
            case "speedx" -> withCurrent(List.of("-1", "0", "1"),
                    head == null ? null : HeadUtil.format(head.options().speedX()));
            case "speedz" -> withCurrent(List.of("-1", "0", "1"),
                    head == null ? null : HeadUtil.format(head.options().speedZ()));
            case "bobheight" -> withCurrent(List.of("0", "0.1", "0.25", "0.5"),
                    head == null ? null : HeadUtil.format(head.options().bobHeight()));
            case "bobperiod" -> withCurrent(List.of("40", "60", "100", "200"),
                    head == null ? null : String.valueOf(head.options().bobPeriod()));
            case "brightness" -> List.of("15/15", "15/0", "0/15", "10/10", "0/0");
            case "range" -> List.of("32", "64", "128", "256");
            case "offsety" -> List.of("0.5", "1", "1.5", "2");
            case "refresh" -> List.of("0", "20", "100", "200");
            case "provider" -> PROVIDERS;
            case "link" -> {
                List<String> names = new ArrayList<>(List.of("none"));
                names.addAll(runtime.linkableHolograms());
                yield names;
            }
            case "follow" -> BOOLEANS;
            case "interaction", "hologram" -> BOOLEANS;
            case "item" -> itemSuggestions(current);
            default -> List.of();
        };
    }

    private static List<String> itemSuggestions(String prefix) {
        if (prefix.isEmpty()) {
            return List.of("player_head");
        }
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (Material material : Material.values()) {
            if (!material.isLegacy() && material.isItem() && !material.isAir()) {
                String name = material.name().toLowerCase(Locale.ROOT);
                if (name.startsWith(lower)) {
                    result.add(name);
                    if (result.size() >= MAX_ITEM_SUGGESTIONS) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static List<String> withCurrent(List<String> base, String current) {
        if (current == null || base.contains(current)) {
            return base;
        }
        List<String> result = new ArrayList<>(base);
        result.add(0, current);
        return result;
    }
}