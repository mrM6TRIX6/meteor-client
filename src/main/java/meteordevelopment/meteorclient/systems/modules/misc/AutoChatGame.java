/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.MessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.RangeSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Range;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoChatGame extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Range> delay = sgGeneral.add(new RangeSetting.Builder()
        .name("delay")
        .description("Delay in ticks before send chat answer.")
        .defaultValue(Range.of(0, 10))
        .min(0)
        .sliderMax(200)
        .build()
    );
    
    private final Setting<Boolean> globalChat = sgGeneral.add(new BoolSetting.Builder()
        .name("global-chat")
        .description("Send answers to the global chat using the prefix \"!\".")
        .defaultValue(false)
        .build()
    );
    
    private final Pattern pattern = Pattern.compile("(\\d+)\\s*([+\\-*/])\\s*(\\d+)");
    private final Timer timer = new Timer();
    
    public AutoChatGame() {
        super(Categories.Misc, "auto-chat-game", "Automatically solves the chat games on MineBlaze/DexLand.");
    }
    
    @EventHandler
    private void onMessageReceive(MessageEvent.Receive event) {
        String message = event.getMessage().getString();
        if (message.startsWith("Chat Game » Решите пример")) {
            Matcher matcher = pattern.matcher(message);
            
            if (matcher.find()) {
                StringBuilder result = new StringBuilder();
                int firstNumber = Integer.parseInt(matcher.group(1));
                int secondNumber = Integer.parseInt(matcher.group(3));
                String operator = matcher.group(2);
                
                if (globalChat.get()) {
                    result.append("!");
                }
                result.append(calculate(firstNumber, secondNumber, operator));
                
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (mc.player != null) {
                            MeteorClient.LOG.info("Message sent");
                            ChatUtils.sendPlayerMsg(result.toString());
                        }
                    }
                }, delay.get().getRandomValue() * 50L);
            }
        }
    }
    
    private int calculate(int a, int b, String operator) {
        return switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> b != 0 ? a / b : 0;
            default -> throw new IllegalArgumentException("Unsupported operator: '" + operator + "'.");
        };
    }
    
}
