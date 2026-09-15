package com.mygame;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.mygame.managers.VkPlayLaunchArgs;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class DesktopLauncher {
    public static void main(String[] args) {
        VkPlayLaunchArgs.parse(args);
        BufferedImage[] icons = null;
        try {
            icons = new BufferedImage[]{
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon16.png")),
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon32.png")),
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon64.png")),
                ImageIO.read(DesktopLauncher.class.getResourceAsStream("/icon128.png"))
            };
            System.out.println("[DesktopLauncher] Icons loaded");
        } catch (Exception e) {
            System.err.println("[DesktopLauncher] Failed to load icons: " + e.getMessage());
        }

        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Exorcist");

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode mode = gd.getDisplayMode();
        settings.setResolution(mode.getWidth(), mode.getHeight());
        settings.setFrequency(mode.getRefreshRate());

        // Один полноэкранный режим
        settings.setFullscreen(true);
        settings.setResizable(false);

        settings.setVSync(true);
        settings.setSamples(4);
        settings.setUseInput(true);
        if (icons != null) {
            settings.setIcons(icons);
        }

        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start(JmeContext.Type.Display);
    }
}