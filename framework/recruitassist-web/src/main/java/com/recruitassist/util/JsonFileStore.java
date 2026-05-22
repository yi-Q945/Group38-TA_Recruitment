package com.recruitassist.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class JsonFileStore {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ConcurrentHashMap<Path, ReentrantReadWriteLock> pathLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, FileCacheEntry> fileCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DirectoryCacheEntry> directoryCache = new ConcurrentHashMap<>();

    public <T> List<T> readAll(Path directory, Class<T> clazz) {
        if (directory == null || Files.notExists(directory)) {
            return List.of();
        }

        Path normalizedDirectory = normalize(directory);
        String cacheKey = normalizedDirectory + "::" + clazz.getName();
        try {
            long directoryModifiedAt = Files.getLastModifiedTime(normalizedDirectory).toMillis();
            DirectoryCacheEntry cached = directoryCache.get(cacheKey);
            if (cached != null && cached.directoryModifiedAt == directoryModifiedAt) {
                @SuppressWarnings("unchecked")
                List<T> cachedValues = (List<T>) cached.values;
                return cachedValues;
            }

            try (Stream<Path> paths = Files.list(normalizedDirectory)) {
                List<T> values = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .sorted()
                        .map(path -> readLenient(path, clazz))
                        .filter(Objects::nonNull)
                        .toList();
                directoryCache.put(cacheKey, new DirectoryCacheEntry(directoryModifiedAt, values));
                return values;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read directory: " + normalizedDirectory, ex);
        }
    }

    public <T> T read(Path file, Class<T> clazz) {
        return read(file, (Type) clazz);
    }

    public <T> T read(Path file, Type type) {
        if (file == null || Files.notExists(file)) {
            return null;
        }

        Path normalized = normalize(file);
        ReentrantReadWriteLock lock = lockFor(normalized);
        lock.readLock().lock();
        try {
            long modifiedAt = Files.getLastModifiedTime(normalized).toMillis();
            long size = Files.size(normalized);
            FileCacheEntry cached = fileCache.get(normalized);
            if (cached != null && cached.modifiedAt == modifiedAt && cached.size == size && cached.type.equals(type)) {
                @SuppressWarnings("unchecked")
                T cachedValue = (T) cached.value;
                return cachedValue;
            }

            try (Reader reader = Files.newBufferedReader(normalized, StandardCharsets.UTF_8)) {
                T parsedValue = gson.fromJson(reader, type);
                fileCache.put(normalized, new FileCacheEntry(type, modifiedAt, size, parsedValue));
                return parsedValue;
            }
        } catch (IOException | JsonSyntaxException ex) {
            throw new IllegalStateException("Failed to read JSON file: " + normalized, ex);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void write(Path file, Object value) {
        Path normalized = normalize(file);
        ReentrantReadWriteLock lock = lockFor(normalized);
        lock.writeLock().lock();
        try {
            withExclusiveFileLock(normalized, () -> {
                writeWithoutProcessLock(normalized, value);
                return null;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void writeWithCallerFileLock(Path file, Object value) {
        Path normalized = normalize(file);
        ReentrantReadWriteLock lock = lockFor(normalized);
        lock.writeLock().lock();
        try {
            writeWithoutProcessLock(normalized, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void writeWithoutProcessLock(Path normalized, Object value) {
        Path tempFile = null;
        try {
            Path parent = normalized.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            tempFile = Files.createTempFile(parent, normalized.getFileName().toString(), ".tmp");
            try (Writer writer = Files.newBufferedWriter(
                    tempFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                gson.toJson(value, writer);
            }

            try {
                Files.move(tempFile, normalized, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, normalized, StandardCopyOption.REPLACE_EXISTING);
            }

            fileCache.remove(normalized);
            invalidateDirectoryCache(parent);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write JSON file: " + normalized, ex);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Best-effort cleanup; the original write exception is more important.
                }
            }
        }
    }

    public void appendLine(Path file, String line) {
        Path normalized = normalize(file);
        ReentrantReadWriteLock lock = lockFor(normalized);
        lock.writeLock().lock();
        try {
            withExclusiveFileLock(normalized, () -> {
                appendLineWithoutProcessLock(normalized, line);
                return null;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> T withExclusiveFileLock(Path file, Supplier<T> action) {
        Path normalized = normalize(file);
        Path parent = normalized.getParent();
        Path lockFile = normalized.resolveSibling(normalized.getFileName().toString() + ".lock");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (FileChannel channel = FileChannel.open(
                    lockFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return action.get();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to lock file: " + normalized, ex);
        }
    }

    private void appendLineWithoutProcessLock(Path normalized, String line) {
        try {
            Path parent = normalized.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    normalized,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.SYNC);
            fileCache.remove(normalized);
            invalidateDirectoryCache(parent);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to append file: " + normalized, ex);
        }
    }

    private <T> T readLenient(Path file, Class<T> clazz) {
        try {
            return read(file, clazz);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private void invalidateDirectoryCache(Path directory) {
        if (directory == null) {
            return;
        }
        String prefix = normalize(directory).toString() + "::";
        directoryCache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private ReentrantReadWriteLock lockFor(Path path) {
        return pathLocks.computeIfAbsent(path, unused -> new ReentrantReadWriteLock());
    }

    private record FileCacheEntry(Type type, long modifiedAt, long size, Object value) {
    }

    private record DirectoryCacheEntry(long directoryModifiedAt, List<?> values) {
    }
}
