package me.cubix.ui;

import me.cubix.world.WorldInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MenuState {
    public MenuScreen screen = MenuScreen.MAIN;

    public boolean showCelsius = true;
    public float mouseSensitivity = 0.12f;
    public String language = I18n.KO_KR;

    public final List<WorldInfo> worlds = new ArrayList<>();
    public int selectedWorld = -1;

    public String newWorldName = "";
    public String newWorldSeed = String.valueOf(new Random().nextLong());

    public int pendingDelete = -1;
    public int playerHeight = 175;
}
