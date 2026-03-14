//package com.zerofall.ezstorage.events;
//
//import moddedmite.rustedironcore.api.event.Handlers;
//import moddedmite.rustedironcore.api.event.listener.IKeybindingListener;
//import net.fabricmc.api.Environment;
//import net.fabricmc.api.EnvType;
//import net.minecraft.KeyBinding;
//import org.lwjgl.input.Keyboard;
//
//import java.util.function.Consumer;
//
//public class EZEventHandler {
//
//    @Environment(EnvType.CLIENT)
//    public void initKeybinds() {
//        Handlers.Keybinding.register(new IKeybindingListener() {
//            @Override
//            public void onKeybindingRegister(Consumer<KeyBinding> registry) {
//                registry.accept(new KeyBinding(
//                    "key.ezstorage.open_terminal",
//                    Keyboard.CHAR_NONE));
//            }
//        });
//    }
//}