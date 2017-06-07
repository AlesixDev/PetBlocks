package com.github.shynixn.petblocks.lib;

import net.minecraft.server.v1_12_R1.EntityTypes;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@SuppressWarnings("unused")
public enum LightRegistry {
    RABBIT(101, "RABBIT", "Rabbit", "rabbit", "EntityRabbit"),
    HORSE(100, "HORSE", "Horse", "Horse", "EntityHorse"),
    ZOMBIE(54, "ZOMBIE", "Zombie", "zombie", "EntityZombie");
    final int entityId;
    final String name;
    final String saveGame_18_19_10;
    final String saveGame_11;
    final Class<?> nmsClass;

    static EntityRegistry entityRegistry;

    LightRegistry(int entityId, String name, String saveGame_18_19_10, String saveGame_11, String nmsClassName) {
        try {
            this.entityId = entityId;
            this.name = name;
            this.saveGame_18_19_10 = saveGame_18_19_10;
            this.saveGame_11 = saveGame_11;
            this.nmsClass = Class.forName("net.minecraft.server." + getServerVersion() + '.' + nmsClassName);
        } catch (final Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "Wrong PowerRegistryType configuration. ", ex);
            throw new RuntimeException(ex);
        }
    }

    private static String getServerVersion() throws Exception {
        return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }

    public static void unregister() {
        try {
            getRegistry().unregister();
        } catch (final Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to unregister entity! ", ex);
        }
    }

    public void register(String customEntityClazzName) {
        try {
            this.register(Class.forName(customEntityClazzName.replace("VERSION", getServerVersion())));
        } catch (final Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to register entity! ", ex);
        }
    }

    public void register(Class<?> customEntityClazz) {
        try {
            getRegistry().registerCustomEntity(customEntityClazz, this);
        } catch (final Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to register entity!", ex);
        }
    }

    private static EntityRegistry getRegistry() throws Exception {
        if (entityRegistry == null) {
            return new WrappedEntityRegistry();
        }
        return entityRegistry;
    }

    interface EntityRegistry {
        void registerCustomEntity(Class<?> customEntityClazz, LightRegistry powerRegistry) throws Exception;

        void unregister() throws Exception;
    }

    private static class WrappedEntityRegistry implements EntityRegistry {
        private static final Map<Class<?>, LightRegistry> types = new HashMap<>();
        private static final List<Object> saveKey = new ArrayList<>();

        @Override
        public void registerCustomEntity(Class<?> customEntityClazz, LightRegistry powerRegistry) throws Exception {
            types.put(customEntityClazz, powerRegistry);
            this.modify(customEntityClazz, powerRegistry.saveGame_18_19_10, powerRegistry.entityId);
        }

        @Override
        public void unregister() throws Exception {
            final Class<?> entityTypeClazz = Class.forName("net.minecraft.server.VERSION.EntityTypes".replace("VERSION", getServerVersion()));
            if (!getServerVersion().equals("v1_11_R1") && !getServerVersion().equals("v1_12_R1")) {
                for (final Class<?> customEntityClazz : types.keySet()) {
                    final LightRegistry powerRegistry = types.get(customEntityClazz);
                    this.<Class<?>, String>getMap(entityTypeClazz, "d").remove(customEntityClazz);
                    this.<Class<?>, String>getMap(entityTypeClazz, "f").remove(customEntityClazz);
                    this.modify(powerRegistry.nmsClass, powerRegistry.saveGame_18_19_10, powerRegistry.entityId);
                }
            } else {
                final Field fieldRegistry = entityTypeClazz.getField("b");
                fieldRegistry.setAccessible(true);
                final Object registry = fieldRegistry.get(null);
                final Field fieldMap = registry.getClass().getSuperclass().getDeclaredField("c");
                fieldMap.setAccessible(true);
                final Map<?, ?> map = (Map<?, ?>) fieldMap.get(registry);
                for (final Object key : saveKey) {
                    map.remove(key);
                }
                saveKey.clear();
            }
        }

        private void modify(Class<?> clazz, String saveGameId, int entityId) throws Exception {
            final Class<?> entityTypeClazz = Class.forName("net.minecraft.server.VERSION.EntityTypes".replace("VERSION", getServerVersion()));
            if (!getServerVersion().equals("v1_11_R1") && !getServerVersion().equals("v1_12_R1")) {
                this.<String, Class<?>>getMap(entityTypeClazz, "c").put(saveGameId, clazz);
                this.<Class<?>, String>getMap(entityTypeClazz, "d").put(clazz, saveGameId);
                this.<Integer, Class<?>>getMap(entityTypeClazz, "e").put(entityId, clazz);
                this.<Class<?>, Integer>getMap(entityTypeClazz, "f").put(clazz, entityId);
                this.<String, Integer>getMap(entityTypeClazz, "f").put(saveGameId, entityId);
            } else {
                final Field field = entityTypeClazz.getField("b");
                field.setAccessible(true);
                final Object registry = field.get(null);
                final Object key = this.generateMinecraftKey(saveGameId);
                final Method add = registry.getClass().getDeclaredMethod("a", Integer.TYPE, Object.class, Object.class);
                add.setAccessible(true);
                add.invoke(registry, Integer.valueOf(entityId), key, clazz);
                saveKey.add(key);
            }
        }

        private Object generateMinecraftKey(String id) throws Exception {
            final Class<?> minecraftKeyGeneration = Class.forName("net.minecraft.server.VERSION.MinecraftKey".replace("VERSION", getServerVersion()));
            return ReflectionLib.invokeConstructor(minecraftKeyGeneration, new String [] {"PetBlocks", id});
        }

        @SuppressWarnings("unchecked")
        private <T, G> Map<T, G> getMap(Class<?> clazz, String name) throws IllegalAccessException, NoSuchFieldException {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return (Map<T, G>) field.get(null);
        }
    }
}
