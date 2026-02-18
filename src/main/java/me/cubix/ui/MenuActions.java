package me.cubix.ui;


import me.cubix.world.WorldInfo;

public interface MenuActions {
    void startSingleplayer(WorldInfo world);
    void backToMenu();
    void quit();
}
