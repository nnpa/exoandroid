package com.mygame;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.jme3.app.AndroidHarness;

public class ExorcistActivity extends AndroidHarness {
    private static ExorcistActivity instance;
    private View currentView;

    public ExorcistActivity() {
        appClass = "com.mygame.Main";
    }

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        // Сохраняем корневое view для использования
        currentView = getWindow().getDecorView().getRootView();
        System.out.println("[ExorcistActivity] onCreate - view saved");
    }

    public static void showKeyboard() {
        if (instance == null) {
            System.out.println("[ExorcistActivity] instance is null");
            return;
        }
        try {
            // Попробуем получить текущий фокус
            View view = instance.getCurrentFocus();
            if (view == null) {
                view = instance.currentView;
            }
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) instance.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    // Показываем клавиатуру
                    boolean result = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                    System.out.println("[ExorcistActivity] Keyboard shown: " + result);
                } else {
                    System.out.println("[ExorcistActivity] InputMethodManager is null");
                }
            } else {
                System.out.println("[ExorcistActivity] view is null");
            }
        } catch (Exception e) {
            System.out.println("[ExorcistActivity] Error showing keyboard: " + e.getMessage());
        }
    }

    public static void hideKeyboard() {
        if (instance == null) return;
        try {
            View view = instance.getCurrentFocus();
            if (view == null) {
                view = instance.currentView;
            }
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) instance.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    System.out.println("[ExorcistActivity] Keyboard hidden");
                }
            }
        } catch (Exception e) {
            System.out.println("[ExorcistActivity] Error hiding keyboard: " + e.getMessage());
        }
    }
}