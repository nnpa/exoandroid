package com.mygame;
import javafx.application.Application;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;

public class DesktopLauncher {
    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Exorcist");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);
        settings.setSamples(4);
        settings.setUseInput(true);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.Display); // или просто app.start()
    }
}