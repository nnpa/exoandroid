package com.mygame.managers;

/**
 * Разбирает аргументы командной строки, которые VK Play Игровой центр
 * передаёт при запуске игры (--sz_pers_id, --sz_token), плюс
 * собственный флаг --test для локальной разработки без VK Play.
 */
public class VkPlayLaunchArgs {

    private static String persId = null;
    private static String token = null;
    private static boolean testMode = false;

    public static void parse(String[] args) {

        if (args == null) {
            return;
        }

        for (String arg : args) {

            if (arg.startsWith("--sz_pers_id=")) {
                persId = arg.substring("--sz_pers_id=".length());

            } else if (arg.startsWith("--sz_token=")) {
                token = arg.substring("--sz_token=".length());

            } else if (arg.equals("--test") || arg.equals("-test")) {
                testMode = true;
            }
        }

        System.out.println(
                "[VkPlayLaunchArgs] persId=" + persId
                + " testMode=" + testMode
        );
    }

    public static String getPersId() {
        return persId;
    }

    public static String getToken() {
        return token;
    }

    public static boolean isTestMode() {
        return testMode;
    }

    public static boolean hasVkPlayData() {
        return persId != null && !persId.isEmpty();
    }
}
