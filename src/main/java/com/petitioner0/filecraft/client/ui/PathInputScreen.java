package com.petitioner0.filecraft.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PathInputScreen extends Screen {
    private final Consumer<String> onConfirm;
    private EditBox pathBox;

    public PathInputScreen(Component title, Consumer<String> onConfirm) {
        super(title);
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int w = 360;
        int x = (this.width - w) / 2;
        int y = this.height / 3;

        pathBox = new EditBox(this.font, x, y, w, 20, Component.translatable("screen.filecraft.path_box"));
        pathBox.setMaxLength(4096);
        pathBox.setValue("");
        this.addRenderableWidget(pathBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
                .bounds(x, y + 28, 170, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x + 190, y + 28, 170, 20).build());

        this.setInitialFocus(pathBox);
    }

    private void confirm() {
        String path = pathBox.getValue().trim();
        if (!path.isEmpty()) onConfirm.accept(path);
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 回车提交
        if (keyCode == 257 /* Enter */) { confirm(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }
}
