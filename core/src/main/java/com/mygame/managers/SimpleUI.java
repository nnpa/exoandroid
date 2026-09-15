package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.font.BitmapText;
import com.jme3.font.BitmapFont;
import com.mygame.Main;
import com.mygame.managers.GameManager.GameState;

public class SimpleUI {

    private SimpleApplication app;
    private Node guiNode;

    // Фоны окон
    private Geometry loginBg;
    private Geometry registerBg;

    // Фоны полей ввода
    private Geometry loginFieldBg, passFieldBg, emailFieldBg, regLoginFieldBg, regPassFieldBg;

    // Тексты
    private BitmapText loginTitle, loginLabel, loginFieldText, passLabel, passFieldText;
    private BitmapText loginBtnText, registerBtnText;
    private BitmapText registerTitle, emailLabel, emailFieldText, regLoginLabel, regLoginFieldText;
    private BitmapText regPassLabel, regPassFieldText, regSubmitBtnText, backBtnText;

    // Прямоугольники для кликов
    private Rect loginFieldRect, passFieldRect, loginBtnRect, registerBtnRect;
    private Rect emailFieldRect, regLoginFieldRect, regPassFieldRect, regSubmitBtnRect, backBtnRect;

    // Данные полей
    private String loginInput = "", passInput = "", emailInput = "", regLoginInput = "", regPassInput = "";
    private enum ActiveField { NONE, LOGIN, PASS, EMAIL, REG_LOGIN, REG_PASS }
    private ActiveField activeField = ActiveField.NONE;
    private boolean isLoginScreen = true;

    private int screenWidth, screenHeight;

    private static class Rect {
        float x, y, w, h;
        Rect(float x, float y, float w, float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        boolean contains(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    public SimpleUI(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();
        this.screenWidth = (int) app.getCamera().getWidth();
        this.screenHeight = (int) app.getCamera().getHeight();
        app.getInputManager().setCursorVisible(true);
    }

    public void initialize() {
        System.out.println("[CustomUI] Init");
        createLoginScreen();
        createRegisterScreen();
        hideAll();
        showLoginScreen();

        // Обработка кликов мыши через RawInputListener, используя getCursorPosition()
        app.getInputManager().addRawInputListener(new RawInputListener() {
            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onMouseMotionEvent(MouseMotionEvent evt) {}
            @Override public void onMouseButtonEvent(MouseButtonEvent evt) {
                if (evt.isPressed() && evt.getButtonIndex() == 0) {
                    Vector2f cursor = app.getInputManager().getCursorPosition();
                    handleMouseClick(cursor.x, cursor.y);
                }
            }
            @Override public void onKeyEvent(KeyInputEvent evt) {
                if (evt.isPressed()) {
                    char c = evt.getKeyChar();
                    if (c != 0 && c != 8) { // не backspace
                        appendChar(c);
                    } else if (evt.getKeyCode() == 8) { // backspace
                        deleteLastChar();
                    }
                }
            }
            @Override public void onTouchEvent(TouchEvent evt) {}
        });
    }

    private void createLoginScreen() {
        int winW = 400, winH = 250;
        float x = (screenWidth - winW) / 2f;
        float y = (screenHeight - winH) / 2f;

        loginBg = createQuad(x, y, winW, winH, new ColorRGBA(0.1f, 0.1f, 0.25f, 0.95f));
        loginBg.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(loginBg);

        loginTitle = createText("Login", 28, ColorRGBA.White, x + 160, y + 220);
        loginLabel = createText("Login:", 18, ColorRGBA.White, x + 30, y + 170);
        passLabel = createText("Password:", 18, ColorRGBA.White, x + 30, y + 130);

        float fieldX = x + 110, fieldY = y + 170, fieldW = 180, fieldH = 25;
        loginFieldBg = createQuad(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10, new ColorRGBA(0.2f, 0.2f, 0.4f, 1f));
        loginFieldBg.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(loginFieldBg);
        loginFieldText = createText("", 18, ColorRGBA.White, fieldX, fieldY + 5);
        loginFieldRect = new Rect(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10);

        fieldX = x + 110; fieldY = y + 130;
        passFieldBg = createQuad(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10, new ColorRGBA(0.2f, 0.2f, 0.4f, 1f));
        passFieldBg.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(passFieldBg);
        passFieldText = createText("", 18, ColorRGBA.White, fieldX, fieldY + 5);
        passFieldRect = new Rect(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10);

        float btnY = y + 70;
        loginBtnText = createText("[ Login ]", 20, ColorRGBA.Green, x + 70, btnY);
        loginBtnRect = new Rect(x + 70, btnY, loginBtnText.getLineWidth(), 30);

        registerBtnText = createText("[ Register ]", 20, ColorRGBA.Blue, x + 200, btnY);
        registerBtnRect = new Rect(x + 200, btnY, registerBtnText.getLineWidth(), 30);
    }

    private void createRegisterScreen() {
        int winW = 400, winH = 300;
        float x = (screenWidth - winW) / 2f;
        float y = (screenHeight - winH) / 2f;

        registerBg = createQuad(x, y, winW, winH, new ColorRGBA(0.1f, 0.1f, 0.25f, 0.95f));
        registerBg.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(registerBg);

        registerTitle = createText("Registration", 28, ColorRGBA.White, x + 130, y + 270);
        emailLabel = createText("Email:", 18, ColorRGBA.White, x + 30, y + 225);
        regLoginLabel = createText("Login:", 18, ColorRGBA.White, x + 30, y + 180);
        regPassLabel = createText("Password:", 18, ColorRGBA.White, x + 30, y + 135);

        float fieldW = 180, fieldH = 25;
        float fieldX, fieldY;

        fieldX = x + 110; fieldY = y + 225;
        emailFieldBg = createQuad(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10, new ColorRGBA(0.2f, 0.2f, 0.4f, 1f));
        emailFieldBg.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(emailFieldBg);
        emailFieldText = createText("", 18, ColorRGBA.White, fieldX, fieldY + 5);
        emailFieldRect = new Rect(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10);

        fieldX = x + 110; fieldY = y + 180;
        regLoginFieldBg = createQuad(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10, new ColorRGBA(0.2f, 0.2f, 0.4f, 1f));
        regLoginFieldBg.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(regLoginFieldBg);
        regLoginFieldText = createText("", 18, ColorRGBA.White, fieldX, fieldY + 5);
        regLoginFieldRect = new Rect(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10);

        fieldX = x + 110; fieldY = y + 135;
        regPassFieldBg = createQuad(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10, new ColorRGBA(0.2f, 0.2f, 0.4f, 1f));
        regPassFieldBg.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(regPassFieldBg);
        regPassFieldText = createText("", 18, ColorRGBA.White, fieldX, fieldY + 5);
        regPassFieldRect = new Rect(fieldX - 5, fieldY - 5, fieldW + 10, fieldH + 10);

        float btnY = y + 70;
        regSubmitBtnText = createText("[ Register ]", 18, ColorRGBA.Green, x + 40, btnY);
        regSubmitBtnRect = new Rect(x + 40, btnY, regSubmitBtnText.getLineWidth(), 30);

        backBtnText = createText("[ Back ]", 18, ColorRGBA.Red, x + 280, btnY);
        backBtnRect = new Rect(x + 280, btnY, backBtnText.getLineWidth(), 30);
    }

    private Geometry createQuad(float x, float y, float w, float h, ColorRGBA color) {
        Quad q = new Quad(w, h);
        Geometry geom = new Geometry("Quad", q);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);
        geom.setLocalTranslation(x, y, 0);
        return geom;
    }

    private BitmapText createText(String text, float size, ColorRGBA color, float x, float y) {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        BitmapText t = new BitmapText(font);
        t.setText(text);
        t.setSize(size);
        t.setColor(color);
        t.setLocalTranslation(x, y, 0);
        t.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(t);
        return t;
    }

    private void hideAll() {
        setVis(loginBg, false);
        setVis(loginTitle, false);
        setVis(loginLabel, false);
        setVis(loginFieldText, false);
        setVis(loginFieldBg, false);
        setVis(passLabel, false);
        setVis(passFieldText, false);
        setVis(passFieldBg, false);
        setVis(loginBtnText, false);
        setVis(registerBtnText, false);

        setVis(registerBg, false);
        setVis(registerTitle, false);
        setVis(emailLabel, false);
        setVis(emailFieldText, false);
        setVis(emailFieldBg, false);
        setVis(regLoginLabel, false);
        setVis(regLoginFieldText, false);
        setVis(regLoginFieldBg, false);
        setVis(regPassLabel, false);
        setVis(regPassFieldText, false);
        setVis(regPassFieldBg, false);
        setVis(regSubmitBtnText, false);
        setVis(backBtnText, false);
    }

    public void showLoginScreen() {
        isLoginScreen = true;
        setVis(loginBg, true);
        setVis(loginTitle, true);
        setVis(loginLabel, true);
        setVis(loginFieldText, true);
        setVis(loginFieldBg, true);
        setVis(passLabel, true);
        setVis(passFieldText, true);
        setVis(passFieldBg, true);
        setVis(loginBtnText, true);
        setVis(registerBtnText, true);

        setVis(registerBg, false);
        setVis(registerTitle, false);
        setVis(emailLabel, false);
        setVis(emailFieldText, false);
        setVis(emailFieldBg, false);
        setVis(regLoginLabel, false);
        setVis(regLoginFieldText, false);
        setVis(regLoginFieldBg, false);
        setVis(regPassLabel, false);
        setVis(regPassFieldText, false);
        setVis(regPassFieldBg, false);
        setVis(regSubmitBtnText, false);
        setVis(backBtnText, false);
    }

    public void showRegisterScreen() {
        isLoginScreen = false;
        setVis(registerBg, true);
        setVis(registerTitle, true);
        setVis(emailLabel, true);
        setVis(emailFieldText, true);
        setVis(emailFieldBg, true);
        setVis(regLoginLabel, true);
        setVis(regLoginFieldText, true);
        setVis(regLoginFieldBg, true);
        setVis(regPassLabel, true);
        setVis(regPassFieldText, true);
        setVis(regPassFieldBg, true);
        setVis(regSubmitBtnText, true);
        setVis(backBtnText, true);

        setVis(loginBg, false);
        setVis(loginTitle, false);
        setVis(loginLabel, false);
        setVis(loginFieldText, false);
        setVis(loginFieldBg, false);
        setVis(passLabel, false);
        setVis(passFieldText, false);
        setVis(passFieldBg, false);
        setVis(loginBtnText, false);
        setVis(registerBtnText, false);
    }

    private void setVis(Geometry g, boolean vis) {
        if (g != null) g.setCullHint(vis ? Node.CullHint.Dynamic : Node.CullHint.Always);
    }
    private void setVis(BitmapText t, boolean vis) {
        if (t != null) t.setCullHint(vis ? Node.CullHint.Dynamic : Node.CullHint.Always);
    }

    private void handleMouseClick(float mx, float my) {
        if (isLoginScreen) {
            if (loginFieldRect != null && loginFieldRect.contains(mx, my)) {
                activeField = ActiveField.LOGIN;
                updateFieldTexts();
                return;
            }
            if (passFieldRect != null && passFieldRect.contains(mx, my)) {
                activeField = ActiveField.PASS;
                updateFieldTexts();
                return;
            }
            if (loginBtnRect != null && loginBtnRect.contains(mx, my)) {
                System.out.println("[UI] Login clicked");
                handleLogin(loginInput, passInput);
                return;
            }
            if (registerBtnRect != null && registerBtnRect.contains(mx, my)) {
                System.out.println("[UI] Register clicked");
                showRegisterScreen();
                return;
            }
        } else {
            if (emailFieldRect != null && emailFieldRect.contains(mx, my)) {
                activeField = ActiveField.EMAIL;
                updateFieldTexts();
                return;
            }
            if (regLoginFieldRect != null && regLoginFieldRect.contains(mx, my)) {
                activeField = ActiveField.REG_LOGIN;
                updateFieldTexts();
                return;
            }
            if (regPassFieldRect != null && regPassFieldRect.contains(mx, my)) {
                activeField = ActiveField.REG_PASS;
                updateFieldTexts();
                return;
            }
            if (regSubmitBtnRect != null && regSubmitBtnRect.contains(mx, my)) {
                System.out.println("[UI] Register submit");
                handleRegister(emailInput, regLoginInput, regPassInput);
                return;
            }
            if (backBtnRect != null && backBtnRect.contains(mx, my)) {
                System.out.println("[UI] Back clicked");
                showLoginScreen();
                return;
            }
        }
        activeField = ActiveField.NONE;
        updateFieldTexts();
    }

    private void appendChar(char c) {
        if (c == 8) return;
        switch (activeField) {
            case LOGIN: loginInput += c; break;
            case PASS: passInput += c; break;
            case EMAIL: emailInput += c; break;
            case REG_LOGIN: regLoginInput += c; break;
            case REG_PASS: regPassInput += c; break;
            default: return;
        }
        updateFieldTexts();
    }

    private void deleteLastChar() {
        switch (activeField) {
            case LOGIN: if (loginInput.length() > 0) loginInput = loginInput.substring(0, loginInput.length()-1); break;
            case PASS: if (passInput.length() > 0) passInput = passInput.substring(0, passInput.length()-1); break;
            case EMAIL: if (emailInput.length() > 0) emailInput = emailInput.substring(0, emailInput.length()-1); break;
            case REG_LOGIN: if (regLoginInput.length() > 0) regLoginInput = regLoginInput.substring(0, regLoginInput.length()-1); break;
            case REG_PASS: if (regPassInput.length() > 0) regPassInput = regPassInput.substring(0, regPassInput.length()-1); break;
            default: return;
        }
        updateFieldTexts();
    }

    private void updateFieldTexts() {
        String cursor = (activeField != ActiveField.NONE) ? "|" : "";
        if (loginFieldText != null)
            loginFieldText.setText(loginInput + (activeField == ActiveField.LOGIN ? cursor : ""));
        if (passFieldText != null) {
            String stars = new String(new char[passInput.length()]).replace('\0', '*');
            passFieldText.setText(stars + (activeField == ActiveField.PASS ? cursor : ""));
        }
        if (emailFieldText != null)
            emailFieldText.setText(emailInput + (activeField == ActiveField.EMAIL ? cursor : ""));
        if (regLoginFieldText != null)
            regLoginFieldText.setText(regLoginInput + (activeField == ActiveField.REG_LOGIN ? cursor : ""));
        if (regPassFieldText != null) {
            String stars = new String(new char[regPassInput.length()]).replace('\0', '*');
            regPassFieldText.setText(stars + (activeField == ActiveField.REG_PASS ? cursor : ""));
        }
    }

    private void handleLogin(String login, String password) {
        System.out.println("[UI] Login: " + login);
        loadCharacter();
    }

    private void handleRegister(String email, String login, String password) {
        System.out.println("[UI] Register: " + login);
        showLoginScreen();
    }

    private void loadCharacter() {
        hideAll();
        Main main = (Main) app;
        if (main != null) {
            GameManager gm = main.getGameManager();
            if (gm != null) {
                gm.setState(GameState.CITY);
            }
        }
    }

    public void onStateChanged(GameState newState) {
        if (newState == GameState.LOGIN) {
            showLoginScreen();
        } else if (newState == GameState.CITY) {
            hideAll();
        }
    }

    public void update(float tpf) {}
    public void cleanup() {
        guiNode.detachAllChildren();
    }
}