package com.mygame.monsters;

import java.lang.reflect.Constructor;

/**
 * Создаёт экземпляры монстров по имени класса через рефлексию.
 */
public class MonsterFactory {

    /**
     * Создаёт монстра по полному имени класса.
     * Предполагается, что класс имеет конструктор без параметров.
     * @param className полное имя класса (например, "com.mygame.monsters.SkeletonWarrior")
     * @return экземпляр Monster или null при ошибке
     */
    public static Monster createMonster(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            // Проверяем, что класс является наследником Monster
            if (!Monster.class.isAssignableFrom(clazz)) {
                System.err.println("[MonsterFactory] Class " + className + " is not a Monster subclass");
                return null;
            }
            Constructor<?> constructor = clazz.getConstructor();
            Object instance = constructor.newInstance();
            return (Monster) instance;
        } catch (Exception e) {
            System.err.println("[MonsterFactory] Failed to instantiate " + className);
            e.printStackTrace();
            return null;
        }
    }
}