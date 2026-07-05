package me.cubix.world;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public final class WorldInfoStorage {
    private final Path savesDir = Paths.get("saves");

    public void ensure() throws IOException {
        Files.createDirectories(savesDir);
    }

    public List<WorldInfo> listWorlds() throws IOException {
        if (!Files.exists(savesDir)) return List.of();
        try (var stream = Files.list(savesDir)) {
            List<Path> dirs = stream.filter(Files::isDirectory).collect(Collectors.toList());
            List<WorldInfo> out = new ArrayList<>();
            for (Path dir : dirs) {
                Path meta = dir.resolve("meta.properties");
                if (!Files.exists(meta)) continue;

                Properties p = new Properties();
                try (var in = Files.newInputStream(meta)) { p.load(in); }

                String id = dir.getFileName().toString();
                String name = p.getProperty("name", id);
                long seed = Long.parseLong(p.getProperty("seed", "0"));
                out.add(new WorldInfo(id, name, seed, dir));
            }
            out.sort(Comparator.comparing(WorldInfo::name));
            return out;
        }
    }

    public WorldInfo createWorld(String name, long seed) throws IOException {
        ensure();
        String id = UUID.randomUUID().toString();
        Path dir = savesDir.resolve(id);
        Files.createDirectories(dir);

        Properties p = new Properties();
        p.setProperty("name", name);
        p.setProperty("seed", Long.toString(seed));
        p.setProperty("createdAt", Long.toString(System.currentTimeMillis()));
        p.setProperty("dir", dir.toString());

        try (var out = Files.newOutputStream(dir.resolve("meta.properties"))) {
            p.store(out, "world meta");
        }
        return new WorldInfo(id, name, seed, dir);
    }

    public void deleteWorld(WorldInfo info) throws IOException {
        Path dir = savesDir.resolve(info.id()).normalize();

        if (!dir.startsWith(savesDir.normalize())) {
            throw new IOException("Refusing to delete outside savesDir: " + dir);
        }

        if (!Files.exists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {

            @NotNull
            @Override
            public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                try {
                    Files.setAttribute(file, "dos:readonly", false, LinkOption.NOFOLLOW_LINKS);
                } catch (Exception ignored) {}

                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @NotNull
            @Override
            public FileVisitResult postVisitDirectory(@NotNull Path d, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.deleteIfExists(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
