package me.ihqqq.rotatingheads.command;

import me.ihqqq.rotatingheads.config.HeadRepository;
import me.ihqqq.rotatingheads.config.Language;
import me.ihqqq.rotatingheads.model.HeadModels.Brightness;
import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.model.HeadModels.HeadOptions;
import me.ihqqq.rotatingheads.model.HeadModels.Hologram;
import me.ihqqq.rotatingheads.model.HeadModels.Interaction;
import me.ihqqq.rotatingheads.runtime.HeadRuntime;
import me.ihqqq.rotatingheads.util.HeadUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class MmhCommand implements CommandExecutor, TabCompleter {
    private final HeadRepository repository;
    private final HeadRuntime runtime;
    private final Map<String, Head> heads;
    private final String defaultTexture;
    private final Language language;

    public MmhCommand(HeadRepository repository, HeadRuntime runtime,
                      Map<String, Head> heads, String defaultTexture,
                      Language language) {
        this.repository = repository;
        this.runtime = runtime;
        this.heads = heads;
        this.defaultTexture = defaultTexture;
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/mmh reload|list|create|edit|teleport|movehere|delete|benchmark|info");
            return true;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "reload" -> reload(sender);
                case "list" -> list(sender);
                case "info" -> sender.sendMessage(
                        "ParaRotatingHead: " + heads.size() + " configured, "
                                + runtime.activeIds().size() + " indexed.");
                case "create" -> create(sender, args);
                case "edit" -> edit(sender, args);
                case "teleport" -> teleport(sender, args);
                case "movehere" -> move(sender, args);
                case "delete" -> delete(sender, args);
                case "benchmark" -> benchmark(sender, args);
                default -> sender.sendMessage("/mmh reload|list|create|edit|teleport|movehere|delete|benchmark|info");
            }
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(language.message("command.invalid", "reason",
                    exception.getMessage()));
        }
        return true;
    }

    private void reload(CommandSender sender) {
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
        sender.sendMessage("Active heads: " + String.join(", ", heads.keySet()));
    }

    private void create(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        String id = HeadUtil.normalizeId(args[1]);
        if (heads.containsKey(id)) {
            sender.sendMessage(language.message("command.head-already-exists", "id", id));
            return;
        }
        String texture = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : defaultTexture;
        Head head = new Head(id, player.getLocation(), 64,
                new HeadOptions(texture, 2, 1, new Brightness(15, 15)),
                new Interaction(false, List.of(), List.of(), List.of()),
                new Hologram(false, 1, 0, List.of(), 1, false, true,
                        "default", new Brightness(15, 15)));
        heads.put(id, head);
        repository.save(head);
        runtime.register(head);
        sender.sendMessage(language.message("command.create.success", "id", id));
    }

    private void edit(CommandSender sender, String[] args) {
        require(args, 4);
        Head old = requireHead(args[1]);
        Head updated = switch (args[2].toLowerCase()) {
            case "texture" -> oldWithOptions(old, new HeadOptions(
                    String.join(" ", Arrays.copyOfRange(args, 3, args.length)),
                    old.options().scale(), old.options().speed(), old.options().brightness()));
            case "scale" -> oldWithOptions(old, new HeadOptions(old.options().texture(),
                    HeadUtil.boundedDouble(args[3], 0.05, 20), old.options().speed(),
                    old.options().brightness()));
            case "speed" -> oldWithOptions(old, new HeadOptions(old.options().texture(),
                    old.options().scale(), HeadUtil.boundedDouble(args[3], -20, 20),
                    old.options().brightness()));
            case "brightness" -> editBrightness(old, args[3]);
            case "interaction" -> oldWithInteraction(old, Boolean.parseBoolean(args[3]));
            case "hologram" -> oldWithHologram(old, Boolean.parseBoolean(args[3]));
            default -> throw new IllegalArgumentException("unknown option");
        };
        heads.put(updated.id(), updated);
        repository.save(updated);
        runtime.register(updated);
        sender.sendMessage(language.message("command.edit.success", "id", updated.id()));
    }

    private Head editBrightness(Head head, String value) {
        String[] parts = value.split("[/:,]");
        if (parts.length != 2) {
            throw new IllegalArgumentException("brightness uses sky/block, e.g. 15/15");
        }
        return oldWithOptions(head, new HeadOptions(head.options().texture(),
                head.options().scale(), head.options().speed(),
                new Brightness(HeadUtil.boundedInt(parts[0], 0, 15),
                        HeadUtil.boundedInt(parts[1], 0, 15))));
    }

    private Head oldWithOptions(Head head, HeadOptions options) {
        return new Head(head.id(), head.location(), head.displayRange(), options,
                head.interaction(), head.hologram());
    }

    private Head oldWithInteraction(Head head, boolean enabled) {
        Interaction value = new Interaction(enabled, head.interaction().any(),
                head.interaction().left(), head.interaction().right());
        return new Head(head.id(), head.location(), head.displayRange(),
                head.options(), value, head.hologram());
    }

    private Head oldWithHologram(Head head, boolean enabled) {
        Hologram value = new Hologram(enabled, head.hologram().offsetY(),
                head.hologram().refreshTicks(), head.hologram().lines(),
                head.hologram().scale(), head.hologram().seeThrough(),
                head.hologram().shadow(), head.hologram().background(),
                head.hologram().brightness());
        return new Head(head.id(), head.location(), head.displayRange(),
                head.options(), head.interaction(), value);
    }

    private void delete(CommandSender sender, String[] args) {
        require(args, 2);
        String id = args[1];
        if (heads.remove(id) == null) {
            throw new IllegalArgumentException("unknown head: " + id);
        }
        runtime.remove(id);
        repository.remove(id);
        sender.sendMessage(language.message("command.delete.success", "id", id));
    }

    private void teleport(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        player.teleport(requireHead(args[1]).location());
        sender.sendMessage(language.message("command.teleport.success", "id", args[1]));
    }

    private void move(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        Head old = requireHead(args[1]);
        Head updated = old.at(player.getLocation());
        heads.put(updated.id(), updated);
        repository.save(updated);
        runtime.register(updated);
        sender.sendMessage(language.message("command.movehere.success", "id", args[1]));
    }

    private void benchmark(CommandSender sender, String[] args) {
        Player player = player(sender);
        require(args, 2);
        if ("clear".equalsIgnoreCase(args[1])) {
            runtime.clearBenchmarks();
            sender.sendMessage("Benchmark cleared.");
            return;
        }
        int count = args.length > 2 ? HeadUtil.boundedInt(args[2], 1, 500) : 100;
        runtime.spawnBenchmark(requireHead(args[1]), player.getLocation(), count);
        sender.sendMessage("Benchmark spawned " + count + ".");
    }

    private Player player(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("only players can use this command");
        }
        return player;
    }

    private Head requireHead(String id) {
        Head head = heads.get(id);
        if (head == null) {
            throw new IllegalArgumentException("unknown head: " + id);
        }
        return head;
    }

    private void require(String[] args, int count) {
        if (args.length < count) {
            throw new IllegalArgumentException("missing argument");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "list", "create", "edit", "teleport",
                    "movehere", "delete", "benchmark", "info");
        }
        if (args.length == 2) {
            return new ArrayList<>(heads.keySet());
        }
        if (args.length == 3 && "edit".equalsIgnoreCase(args[0])) {
            return List.of("texture", "scale", "speed", "brightness",
                    "interaction", "hologram");
        }
        return List.of();
    }
}