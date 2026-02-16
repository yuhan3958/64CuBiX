package me.cubix.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MenuState {
    public MenuScreen screen = MenuScreen.MAIN;

    public boolean showCelsius = true;
    public float mouseSensitivity = 0.12f;

    public final List<WorldInfo> worlds = new ArrayList<>();
    public int selectedWorld = -1;

    public String newWorldName = "새로운 세계";
    public String newWorldSeed = String.valueOf(new Random().nextLong());

    public int pendingDelete = -1;
    public int playerHeight = 175;
}
