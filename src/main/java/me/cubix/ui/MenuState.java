package me.cubix.ui;

import java.util.ArrayList;
import java.util.List;

public final class MenuState {
    public MenuScreen screen = MenuScreen.MAIN;

    public boolean showCelsius = true;
    public float mouseSensitivity = 0.12f;

    public final List<WorldInfo> worlds = new ArrayList<>();
    public int selectedWorld = -1;

    public String newWorldName = "New World";
    public String newWorldSeed = "12345";

    public int pendingDelete = -1;
}
