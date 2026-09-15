package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.SoftTextDialogInput;
import com.jme3.input.controls.SoftTextDialogInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeSystem;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;

/**
 * Открывает нативный Android-диалог ввода текста/чисел (с настоящей
 * системной клавиатурой) при тапе/клике на Lemur TextField.
 *
 * ПОЧЕМУ ЭТО НУЖНО: TextField в Lemur рисуется прямо на 3D-поверхности
 * (GLSurfaceView), а не является настоящим Android View — системная
 * клавиатура физически не может "прикрепиться" к нему напрямую, ей
 * нужен реальный фокусируемый EditText. Попытка вызвать
 * InputMethodManager.showSoftInput() на самой игровой поверхности
 * (как это часто пытаются сделать) не работает в принципе — именно
 * поэтому "обычный" способ показать клавиатуру тут не срабатывает.
 *
 * JmeSystem.getSoftTextDialogInput() — официальный, штатный механизм
 * jME3 для Android: показывает системный диалог с настоящим EditText
 * поверх игры, клавиатура вызывается автоматически, как для любого
 * обычного Android-приложения.
 */
public class AndroidTextInputHelper {

    /**
     * Открывает числовую клавиатуру (для цены, количества и т.п.).
     */
    public static void attachNumericKeyboard(SimpleApplication app, TextField field, String title) {
        attach(app, field, title, SoftTextDialogInput.NUMERIC_ENTRY_DIALOG);
    }

    /**
     * Открывает обычную текстовую клавиатуру.
     */
    public static void attachTextKeyboard(SimpleApplication app, TextField field, String title) {
        attach(app, field, title, SoftTextDialogInput.TEXT_ENTRY_DIALOG);
    }

    private static void attach(
            SimpleApplication app,
            TextField field,
            String title,
            int dialogType
    ) {

        MouseEventControl.addListenersToSpatial(field, new MouseListener() {

            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial target, Spatial capture) {

                if (!evt.isPressed()) {
                    return;
                }

                SoftTextDialogInput dialogInput = JmeSystem.getSoftTextDialogInput();

                if (dialogInput == null) {
                    /*
                     * На десктопе/в эмуляторе без нативного диалога —
                     * просто оставляем обычный ввод с физической
                     * клавиатуры, ничего не делаем.
                     */
                    return;
                }

                dialogInput.requestDialog(
                        dialogType,
                        title,
                        field.getText(),
                        new SoftTextDialogInputListener() {
                            @Override
                            public void onSoftText(int action, String text) {

                                if (action == SoftTextDialogInputListener.COMPLETE) {

                                    app.enqueue(() -> {
                                        field.setText(text);
                                        return null;
                                    });
                                }
                            }
                        }
                );
            }

            @Override
            public void mouseEntered(MouseMotionEvent evt, Spatial target, Spatial capture) {
            }

            @Override
            public void mouseExited(MouseMotionEvent evt, Spatial target, Spatial capture) {
            }

            @Override
            public void mouseMoved(MouseMotionEvent evt, Spatial target, Spatial capture) {
            }
        });
    }
}
