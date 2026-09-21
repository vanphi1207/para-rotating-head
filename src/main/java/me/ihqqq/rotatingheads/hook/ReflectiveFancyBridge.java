package me.ihqqq.rotatingheads.hook;

import me.ihqqq.rotatingheads.model.Hologram;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;


final class ReflectiveFancyBridge implements FancyBridge {
    private static final String API = "de.oliver.fancyholograms.api.";
    private static final int MILLIS_PER_TICK = 50;

    private final JavaPlugin plugin;
    private final Object manager;
    private final Method getHologram;
    private final Method getHolograms;
    private final Method create;
    private final Method addHologram;
    private final Method removeHologram;
    private final Method hologramName;
    private final Method hologramData;
    private final Method queueUpdate;
    private final Method forceUpdate;
    private final Method dataLocation;
    private final Method dataSetLocation;
    private final Constructor<?> textDataConstructor;
    private final Object transparent;
    private final Set<String> reported = new HashSet<>();

    private ReflectiveFancyBridge(JavaPlugin plugin, Object manager)
            throws ReflectiveOperationException {
        this.plugin = plugin;
        this.manager = manager;
        Class<?> managerType = Class.forName(API + "HologramManager");
        Class<?> hologramType = Class.forName(API + "hologram.Hologram");
        Class<?> dataType = Class.forName(API + "data.HologramData");
        getHologram = managerType.getMethod("getHologram", String.class);
        getHolograms = managerType.getMethod("getHolograms");
        create = managerType.getMethod("create", dataType);
        addHologram = managerType.getMethod("addHologram", hologramType);
        removeHologram = managerType.getMethod("removeHologram", hologramType);
        hologramName = hologramType.getMethod("getName");
        hologramData = hologramType.getMethod("getData");
        queueUpdate = optionalMethod(hologramType, "queueUpdate");
        forceUpdate = optionalMethod(hologramType, "forceUpdate");
        dataLocation = dataType.getMethod("getLocation");
        dataSetLocation = dataType.getMethod("setLocation", Location.class);
        textDataConstructor = Class.forName(API + "data.TextHologramData")
                .getConstructor(String.class, Location.class);
        transparent = staticField(hologramType, "TRANSPARENT");
    }

    static ReflectiveFancyBridge connect(JavaPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("FancyHolograms")) {
            return null;
        }
        try {
            Class<?> api = Class.forName(API + "FancyHologramsPlugin");
            if (!(boolean) api.getMethod("isEnabled").invoke(null)) {
                return null;
            }
            Object instance = api.getMethod("get").invoke(null);
            Object manager = api.getMethod("getHologramManager").invoke(instance);
            return new ReflectiveFancyBridge(plugin, manager);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("FancyHolograms was detected but its API could not be "
                    + "used (" + describe(exception) + "). Is this a supported version?");
            return null;
        }
    }

    @Override
    public Object find(String name) {
        Optional<?> found = (Optional<?>) call(getHologram, manager, name);
        return found.orElse(null);
    }

    @Override
    public Collection<String> names() {
        List<String> names = new ArrayList<>();
        for (Object hologram : (Collection<?>) call(getHolograms, manager)) {
            names.add(String.valueOf(call(hologramName, hologram)));
        }
        return names;
    }

    @Override
    public Location locationOf(Object hologram) {
        return (Location) call(dataLocation, call(hologramData, hologram));
    }

    @Override
    public void move(Object hologram, Location location) {
        call(dataSetLocation, call(hologramData, hologram), location);
        Method update = queueUpdate != null ? queueUpdate : forceUpdate;
        if (update != null) {
            call(update, hologram);
        }
    }

    @Override
    public Object create(String name, Location location, Hologram style,
                         int visibilityDistance) {
        final Object data;
        try {
            data = textDataConstructor.newInstance(name, location);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("could not create hologram data: "
                    + describe(exception), exception);
        }
        boolean nonPersistent = set(data, "setPersistent", false);
        if (!set(data, "setText", new ArrayList<>(style.lines()))) {
            throw new IllegalStateException("FancyHolograms has no usable setText method");
        }
        best(data, "setScale", new Vector3f((float) style.scale()));
        best(data, "setBillboard", Display.Billboard.CENTER);
        best(data, "setSeeThrough", style.seeThrough());
        best(data, "setTextShadow", style.shadow());
        best(data, "setTextUpdateInterval", style.refreshTicks() > 0
                ? style.refreshTicks() * MILLIS_PER_TICK : -1);
        best(data, "setVisibilityDistance", visibilityDistance);
        best(data, "setBrightness", new Display.Brightness(
                style.brightness().block(), style.brightness().sky()));
        best(data, "setBackground", background(style.background()));
        Object hologram = call(create, manager, data);
        if (!nonPersistent) {
            best(hologram, "setPersistent", false);
        }
        call(addHologram, manager, hologram);
        return hologram;
    }

    @Override
    public void remove(Object hologram) {
        call(removeHologram, manager, hologram);
    }

    private Object background(String value) {
        String text = value.trim().toLowerCase(Locale.ROOT);
        if (text.equals("default")) {
            return null;
        }
        if (text.equals("none") || text.equals("transparent")) {
            return transparent != null ? transparent : Color.fromARGB(0);
        }
        try {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            long color = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                return Color.fromRGB((int) (color >> 16) & 255, (int) (color >> 8) & 255,
                        (int) color & 255);
            }
            if (hex.length() == 8) {
                return Color.fromARGB((int) (color >> 24) & 255, (int) (color >> 16) & 255,
                        (int) (color >> 8) & 255, (int) color & 255);
            }
        } catch (NumberFormatException exception) {
        }
        return null;
    }

    private void best(Object target, String name, Object argument) {
        if (!set(target, name, argument) && reported.add(name)) {
            plugin.getLogger().warning("FancyHolograms API has no usable '" + name
                    + "' method; that hologram setting is skipped.");
        }
    }

    private static boolean set(Object target, String name, Object argument) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                    && accepts(method.getParameterTypes()[0], argument)) {
                try {
                    method.invoke(target, argument);
                    return true;
                } catch (ReflectiveOperationException exception) {
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean accepts(Class<?> type, Object argument) {
        if (argument == null) {
            return !type.isPrimitive();
        }
        return wrap(type).isInstance(argument);
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        return type;
    }

    private static Method optionalMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static Object staticField(Class<?> type, String name) {
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static Object call(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(describe(exception), exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(describe(exception), exception);
        }
    }

    private static String describe(Throwable throwable) {
        Throwable cause = throwable instanceof InvocationTargetException
                && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getClass().getSimpleName() + (cause.getMessage() == null ? ""
                : ": " + cause.getMessage());
    }
}