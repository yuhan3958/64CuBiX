package me.cubix.ui;

import java.nio.file.Path;

public record WorldInfo(String id, String name, long seed, Path dir) {}
