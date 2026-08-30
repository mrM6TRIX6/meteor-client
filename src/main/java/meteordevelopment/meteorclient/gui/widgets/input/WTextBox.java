/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.gui.GuiOverlays;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfFont;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.MacWindowUtil;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.SystemUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class WTextBox extends WWidget {

    private static final Renderer DEFAULT_RENDERER = (context, x, y, text, color) -> Render2D.msdf(MsdfFont.JETBRAINS_MONO_REGULAR, text, (int) x, (int) y, (int) GuiConstants.textSize(), color.getPacked());

    public Runnable action;
    public Runnable actionOnUnfocused;

    protected String text;
    protected String placeholder;
    protected CharFilter filter;

    protected final Renderer renderer;

    protected DoubleList textWidths = new DoubleArrayList();

    protected int cursor;
    protected double textStart;

    protected boolean selecting, doubleClick;
    protected int selectionStart, selectionEnd;
    private int preSelectionCursor;

    private List<String> completions;
    private int completionsStart;
    private WContainer completionsW;

    private boolean cursorVisible;
    private double cursorTimer;
    private double cursorAnimProgress;

    public WTextBox(String text) {
        this(text, null, (t, c) -> true, null);
    }

    public WTextBox(String text, String placeholder) {
        this(text, placeholder, (t, c) -> true, null);
    }

    public WTextBox(String text, CharFilter filter) {
        this(text, null, filter, null);
    }

    public WTextBox(String text, String placeholder, CharFilter filter) {
        this(text, placeholder, filter, null);
    }

    public WTextBox(String text, CharFilter filter, Class<? extends Renderer> renderer) {
        this(text, null, filter, renderer);
    }

    public WTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        this.text = text;
        this.placeholder = placeholder;
        this.filter = filter;

        try {
            this.renderer = renderer != null ? renderer.getDeclaredConstructor().newInstance() : DEFAULT_RENDERER;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        double s = GuiConstants.textHeight();

        width = pad + s + pad;
        height = pad + s + pad;

        calculateTextWidths();

        if (completionsW != null) {
            completionsW.calculateSize();
        }
    }

    
    @Override
    public void calculateWidgetPositions() {
        super.calculateWidgetPositions();
        
        if (completionsW != null) {
            completionsW.x = x;
            completionsW.y = y + height;
            completionsW.calculateWidgetPositions();
        }
    }
    
    @Override
    public void move(double deltaX, double deltaY) {
        super.move(deltaX, deltaY);
        if (completionsW != null) {
            completionsW.move(deltaX, deltaY);
        }
    }
    
    protected double maxTextWidth() {
        return width - pad() * 2;
    }
    
    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        if (mouseOver) {
            if (click.button() == GLFW_MOUSE_BUTTON_RIGHT) {
                if (!text.isEmpty()) {
                    text = "";
                    cursor = 0;
                    selectionStart = 0;
                    selectionEnd = 0;
                    
                    runAction();
                }
            } else if (click.button() == GLFW_MOUSE_BUTTON_LEFT) {
                selecting = true;
                
                double overflowWidth = getOverflowWidthForRender();
                double relativeMouseX = click.x() - x + overflowWidth;
                double pad = pad();
                
                double smallestDifference = Double.MAX_VALUE;
                
                cursor = text.length();
                
                for (int i = 0; i < textWidths.size(); i++) {
                    double difference = Math.abs(textWidths.getDouble(i) + pad - relativeMouseX);
                    
                    if (difference < smallestDifference) {
                        smallestDifference = difference;
                        cursor = i;
                    }
                }
                
                if (doubled && cursor == preSelectionCursor) {
                    doubleClick = true;
                    resetSelection();
                    
                    selectionStart = (cursor - countToNextSpace(true));
                    selectionEnd = cursor = (cursor + countToNextSpace(false));
                    
                    return true;
                }
                
                preSelectionCursor = cursor;
                resetSelection();
                cursorChanged();
            }
            
            setFocused(true);
            return true;
        }
        
        if (focused) {
            setFocused(false);
        }
        
        return false;
    }
    
    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        if (!selecting) {
            return;
        }
        
        double overflowWidth = getOverflowWidthForRender();
        double relativeMouseX = mouseX - x + overflowWidth;
        double pad = pad();
        
        double smallestDifference = Double.MAX_VALUE;
        
        int best = 0;
        for (int i = 0; i < textWidths.size(); i++) {
            double difference = Math.abs(textWidths.getDouble(i) + pad - relativeMouseX);
            
            if (difference < smallestDifference) {
                best = i;
                smallestDifference = difference;
                if (!doubleClick) {
                    if (i < preSelectionCursor) {
                        selectionStart = i;
                        cursor = i;
                    } else if (i > preSelectionCursor) {
                        selectionEnd = i;
                        cursor = i;
                    } else {
                        cursor = preSelectionCursor;
                        resetSelection();
                    }
                }
            }
        }
        
        // Double click selection will select by whole words
        if (doubleClick) {
            if (best < selectionStart) {
                selectionStart = best - countToNextSpace(true, best);
                cursor = selectionStart;
            } else if (best > selectionEnd) {
                selectionEnd = best + countToNextSpace(false, best);
                cursor = selectionEnd;
            } else {
                if (cursor == selectionStart) {
                    int nextRight = countToNextSpace(false);
                    if (best > cursor + nextRight) {
                        selectionStart = cursor = cursor + nextRight + 1;
                        if (selectionStart <= preSelectionCursor && selectionStart + countToNextSpace(false) >= preSelectionCursor) {
                            cursor = selectionEnd;
                        }
                    }
                } else if (cursor == selectionEnd) {
                    int nextLeft = countToNextSpace(true);
                    if (best < cursor - nextLeft) {
                        selectionEnd = cursor = cursor - nextLeft - 1;
                    }
                }
            }
        }
    }
    
    @Override
    public boolean onMouseReleased(Click click) {
        selecting = false;
        doubleClick = false;
        
        if (selectionStart < preSelectionCursor && preSelectionCursor == selectionEnd) {
            cursor = selectionStart;
        } else if (selectionEnd > preSelectionCursor && preSelectionCursor == selectionStart) {
            cursor = selectionEnd;
        }
        
        return false;
    }
    
    @Override
    public boolean onKeyPressed(KeyInput input) {
        if (!focused) {
            return false;
        }
        
        boolean control = MacWindowUtil.IS_MAC ? input.modifiers() == GLFW_MOD_SUPER : input.modifiers() == GLFW_MOD_CONTROL;
        
        if (control && input.key() == GLFW_KEY_C) {
            if (cursor != selectionStart || cursor != selectionEnd) {
                mc.keyboard.setClipboard(text.substring(selectionStart, selectionEnd));
            }
            return true;
        } else if (control && input.key() == GLFW_KEY_X) {
            if (cursor != selectionStart || cursor != selectionEnd) {
                mc.keyboard.setClipboard(text.substring(selectionStart, selectionEnd));
                clearSelection();
            }
            
            return true;
        } else if (control && input.key() == GLFW_KEY_A) {
            cursor = text.length();
            selectionStart = 0;
            selectionEnd = cursor;
        } else if (input.modifiers() == ((MacWindowUtil.IS_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL) | GLFW_MOD_SHIFT) && input.key() == GLFW_KEY_A) {
            resetSelection();
        } else if (input.key() == GLFW_KEY_ENTER || input.key() == GLFW_KEY_KP_ENTER) {
            setFocused(false);
            
            if (actionOnUnfocused != null) {
                actionOnUnfocused.run();
            }
            return true;
        } else if (input.key() == GLFW_KEY_TAB && completionsW != null) {
            String completion = ((ICompletionItem) completionsW.cells.get(getSelectedCompletion()).widget()).getCompletion();
            
            StringBuilder sb = new StringBuilder(text.length() + completion.length() + 1);
            String a = text.substring(0, cursor);
            sb.append(a);
            
            for (int i = 0; i < completion.length() - 1; i++) {
                if (a.endsWith(completion.substring(0, completion.length() - i - 1))) {
                    completion = completion.substring(completion.length() - i - 1);
                    break;
                }
            }
            
            sb.append(completion);
            if (completion.endsWith("(")) {
                sb.append(')');
            }
            
            sb.append(text, cursor, text.length());
            
            text = sb.toString();
            cursor += completion.length();
            resetSelection();
            runAction();
            
            return true;
        }
        
        return onKeyRepeated(input);
    }
    
    @Override
    public boolean onKeyRepeated(KeyInput input) {
        if (!focused) {
            return false;
        }
        
        boolean control = MacWindowUtil.IS_MAC ? input.modifiers() == GLFW_MOD_SUPER : input.modifiers() == GLFW_MOD_CONTROL;
        boolean shift = input.modifiers() == GLFW_MOD_SHIFT;
        boolean controlShift = input.modifiers() == ((SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MacWindowUtil.IS_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL) | GLFW_MOD_SHIFT);
        boolean altShift = input.modifiers() == ((SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT) | GLFW_MOD_SHIFT);
        
        boolean isModifierPressed = input.modifiers() == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MacWindowUtil.IS_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL);
        
        if (control && input.key() == GLFW_KEY_V) {
            clearSelection();
            
            String preText = text;
            String clipboard = mc.keyboard.getClipboard();
            int addedChars = 0;
            
            StringBuilder sb = new StringBuilder(text.length() + clipboard.length());
            sb.append(text);
            
            for (int i = 0; i < clipboard.length(); i++) {
                char c = clipboard.charAt(i);
                if (filter.filter(sb.toString(), c)) {
                    sb.insert(cursor + addedChars, c);
                    addedChars++;
                }
            }
            
            text = sb.toString();
            cursor += addedChars;
            resetSelection();
            
            if (!text.equals(preText)) {
                runAction();
            }
            return true;
        } else {
            if (input.key() == GLFW_KEY_BACKSPACE) {
                if (cursor > 0 && cursor == selectionStart && cursor == selectionEnd) {
                    String preText = text;
                    
                    int count = isModifierPressed
                        ? cursor
                        : (input.modifiers() == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT))
                        ? countToNextSpace(true)
                        : 1;
                    
                    text = text.substring(0, cursor - count) + text.substring(cursor);
                    cursor -= count;
                    resetSelection();
                    
                    if (!text.equals(preText)) {
                        runAction();
                    }
                } else if (cursor != selectionStart || cursor != selectionEnd) {
                    clearSelection();
                }
                
                return true;
            } else if (input.key() == GLFW_KEY_DELETE) {
                if (cursor == selectionStart && cursor == selectionEnd) {
                    if (cursor < text.length()) {
                        String preText = text;
                        
                        int count = isModifierPressed
                            ? text.length() - cursor
                            : (input.modifiers() == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT))
                            ? countToNextSpace(false)
                            : 1;
                        
                        text = text.substring(0, cursor) + text.substring(cursor + count);
                        
                        if (!text.equals(preText)) {
                            runAction();
                        }
                    }
                } else {
                    clearSelection();
                }
                return true;
            } else if (input.key() == GLFW_KEY_LEFT) {
                if (cursor > 0) {
                    // Sets the cursor to just after the next leftmost space
                    if (input.modifiers() == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT)) {
                        cursor -= countToNextSpace(true);
                        resetSelection();
                    }
                    // Sets the cursor to the beginning of the text box
                    else if (isModifierPressed ) {
                        cursor = 0;
                        resetSelection();
                    }
                    // Sets the selection to just after the next leftmost space
                    else if (altShift) {
                        if (cursor == selectionEnd && cursor != selectionStart) {
                            cursor -= countToNextSpace(true);
                            if (cursor >= selectionStart) {
                                selectionEnd = cursor;
                            } else {
                                selectionEnd = selectionStart;
                                selectionStart = cursor;
                            }
                        } else {
                            cursor -= countToNextSpace(true);
                            selectionStart = cursor;
                        }
                    }
                    // Sets the selection to the beginning of the text box
                    else if (controlShift) {
                        if (cursor == selectionEnd && cursor != selectionStart) {
                            selectionEnd = selectionStart;
                        }
                        selectionStart = 0;
                        
                        cursor = 0;
                    }
                    // Moves the selection one character to the left
                    else if (shift) {
                        if (cursor == selectionEnd && cursor != selectionStart) {
                            selectionEnd = cursor - 1;
                        } else {
                            selectionStart = cursor - 1;
                        }
                        
                        cursor--;
                    }
                    // Moves the cursor one character to the left
                    else {
                        if (cursor == selectionEnd && cursor != selectionStart) {
                            cursor = selectionStart;
                        } else {
                            cursor--;
                        }
                        
                        resetSelection();
                    }
                    
                    cursorChanged();
                } else if (selectionStart != selectionEnd && selectionStart == 0 && input.modifiers() == 0) {
                    cursor = 0;
                    resetSelection();
                    cursorChanged();
                }
                
                return true;
            } else if (input.key() == GLFW_KEY_RIGHT) {
                if (cursor < text.length()) {
                    // Sets the cursor to just before the next rightmost space
                    if (input.modifiers() == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT)) {
                        cursor += countToNextSpace(false);
                        resetSelection();
                    }
                    // Sets the cursor to the end of the text box
                    else if (isModifierPressed) {
                        cursor = text.length();
                        resetSelection();
                    }
                    // Sets the selection to just before the next rightmost space
                    else if (altShift) {
                        if (cursor == selectionStart && cursor != selectionEnd) {
                            cursor += countToNextSpace(false);
                            selectionStart = cursor;
                        } else {
                            cursor += countToNextSpace(false);
                            if (cursor <= selectionEnd) {
                                selectionStart = cursor;
                            } else {
                                selectionStart = selectionEnd;
                                selectionEnd = cursor;
                            }
                        }
                    }
                    // Sets the selection to the end of the text box
                    else if (controlShift) {
                        if (cursor == selectionStart && cursor != selectionEnd) {
                            selectionStart = selectionEnd;
                        }
                        cursor = text.length();
                        selectionEnd = cursor;
                    }
                    // Moves the selection one character to the right
                    else if (shift) {
                        if (cursor == selectionStart && cursor != selectionEnd) {
                            selectionStart = cursor + 1;
                        } else {
                            selectionEnd = cursor + 1;
                        }
                        
                        cursor++;
                    }
                    // Moves the cursor one character to the right
                    else {
                        if (cursor == selectionStart && cursor != selectionEnd) {
                            cursor = selectionEnd;
                        } else {
                            cursor++;
                        }
                        
                        resetSelection();
                    }
                    
                    cursorChanged();
                } else if (selectionStart != selectionEnd && selectionEnd == text.length() && input.modifiers() == 0) {
                    cursor = text.length();
                    resetSelection();
                    cursorChanged();
                }
                
                return true;
            } else if (input.key() == GLFW_KEY_DOWN && completionsW != null) {
                int currentI = getSelectedCompletion();
                
                if (currentI == Math.min(5, completions.size() - 1)) {
                    if (completionsStart + 6 < completions.size()) {
                        completionsStart++;
                        createCompletions(completionsStart + currentI);
                    }
                } else {
                    ((ICompletionItem) completionsW.cells.get(currentI).widget()).setSelected(false);
                    ((ICompletionItem) completionsW.cells.get(currentI + 1).widget()).setSelected(true);
                }
                
                return true;
            } else if (input.key() == GLFW_KEY_UP && completionsW != null) {
                int currentI = getSelectedCompletion();
                
                if (currentI == 0) {
                    if (completionsStart > 0) {
                        completionsStart--;
                        createCompletions(completionsStart + currentI);
                    }
                } else {
                    ((ICompletionItem) completionsW.cells.get(currentI).widget()).setSelected(false);
                    ((ICompletionItem) completionsW.cells.get(currentI - 1).widget()).setSelected(true);
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    private int getSelectedCompletion() {
        for (int i = 0; i < completionsW.cells.size(); i++) {
            ICompletionItem item = (ICompletionItem) completionsW.cells.get(i).widget();
            if (!item.isSelected()) {
                continue;
            }
            
            return i;
        }
        
        return -1;
    }
    
    @Override
    public boolean onCharTyped(CharInput input) {
        if (!focused) {
            return false;
        }
        
        if (filter.filter(text, input.codepoint())) {
            clearSelection();
            
            text = text.substring(0, cursor) + input.asString() + text.substring(cursor);
            
            cursor++;
            resetSelection();
            
            runAction();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean render(DrawContext context, double mouseX, double mouseY, double delta) {
        if (isFocused()) {
            GuiKeyEvents.canUseKeys = false;
        }

        if (completionsW != null && focused) {
            WContainer completions = completionsW;
            GuiOverlays.add(() -> completions.render(context, mouseX, mouseY, delta));
        }

        return super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        if (cursorTimer >= 1) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
        } else {
            cursorTimer += delta * 1.75;
        }

        renderBackground(false, false);

        double pad = pad();
        double overflowWidth = getOverflowWidthForRender();

        pushScissor(context, x + pad, y + pad, width - pad * 2, height - pad * 2);

        // Text content
        if (!text.isEmpty()) {
            this.renderer.render(context, x + pad - overflowWidth, y + pad, text, GuiConstants.TEXT);
        } else if (placeholder != null) {
            this.renderer.render(context, x + pad - overflowWidth, y + pad, placeholder, GuiConstants.PLACEHOLDER);
        }

        // Text highlighting
        if (focused && (cursor != selectionStart || cursor != selectionEnd)) {
            double selStart = x + pad + getTextWidth(selectionStart) - overflowWidth;
            double selEnd = x + pad + getTextWidth(selectionEnd) - overflowWidth;

            rect(selStart, y + pad, selEnd - selStart, GuiConstants.textHeight(), GuiConstants.TEXT_HIGHLIGHT);
        }

        // Cursor
        cursorAnimProgress += delta * 10 * (focused && cursorVisible ? 1 : -1);
        cursorAnimProgress = MathHelper.clamp(cursorAnimProgress, 0, 1);

        if ((focused && cursorVisible) || cursorAnimProgress > 0) {
            Render2D.rect((float) (x + pad + getTextWidth(cursor) - overflowWidth), (float) (y + pad), (float) GuiConstants.scale(1), (float) GuiConstants.textHeight(), GuiConstants.color(GuiConstants.TEXT, (float) cursorAnimProgress));
        }

        popScissor(context);
    }
    
    private void clearSelection() {
        if (selectionStart == selectionEnd) {
            return;
        }
        
        String preText = text;
        
        text = text.substring(0, selectionStart) + text.substring(selectionEnd);
        
        cursor = selectionStart;
        selectionEnd = cursor;
        
        if (!text.equals(preText)) {
            runAction();
        }
    }
    
    private void resetSelection() {
        selectionStart = cursor;
        selectionEnd = cursor;
    }
    
    private int countToNextSpace(boolean toLeft) {
        return countToNextSpace(toLeft, cursor);
    }
    
    private int countToNextSpace(boolean toLeft, int startPos) {
        int count = 0;
        boolean hadNonSpace = false;
        
        for (int i = startPos; toLeft ? i >= 0 : i < text.length(); i += toLeft ? -1 : 1) {
            int j = i;
            if (toLeft) {
                j--;
            }
            
            if (j >= text.length()) {
                continue;
            }
            if (j < 0) {
                break;
            }
            
            if (hadNonSpace && Character.isWhitespace(text.charAt(j))) {
                break;
            } else if (!Character.isWhitespace(text.charAt(j))) {
                hadNonSpace = true;
            }
            
            count++;
        }
        
        return count;
    }
    
    private void calculateTextWidths() {
        textWidths.clear();

        for (int i = 0; i <= text.length(); i++) {
            textWidths.add(MsdfFont.JETBRAINS_MONO_REGULAR.width(text.substring(0, i), (float) GuiConstants.textSize()));
        }
    }
    
    private void runAction() {
        calculateTextWidths();
        cursorChanged();
        
        if (action != null) {
            action.run();
        }
    }
    
    private double textWidth() {
        return textWidths.isEmpty() ? 0 : textWidths.getDouble(textWidths.size() - 1);
    }
    
    private void cursorChanged() {
        double cursor = getCursorTextWidth(-2);
        if (cursor < textStart) {
            textStart -= textStart - cursor;
        }
        
        cursor = getCursorTextWidth(2);
        if (cursor > textStart + maxTextWidth()) {
            textStart += cursor - (textStart + maxTextWidth());
        }
        
        textStart = MathHelper.clamp(textStart, 0, Math.max(textWidth() - maxTextWidth(), 0));
        
        onCursorChanged();
        
        // Completions
        completions = renderer.getCompletions(text, this.cursor);
        completionsStart = 0;
        completionsW = null;
        if (completions != null && !completions.isEmpty()) {
            createCompletions(0);
        }
    }
    
    protected void onCursorChanged() {
        cursorVisible = true;
        cursorTimer = 0;
    }

    private void createCompletions(int selected) {
        completionsW = new WCompletions();

        int max = Math.min(completions.size(), completionsStart + 6);
        for (int i = completionsStart; i < max; i++) {
            Cell<?> cell = completionsW.add(new WCompletionItem(completions.get(i), i == selected)).expandX().padHorizontal(4);
            if (i == max - 1) {
                cell.padBottom(4);
            }
        }

        completionsW.calculateSize();
        completionsW.x = Math.min(Math.max(x - pad() * 2 + getTextWidth(cursor) - getOverflowWidthForRender(), x), x + width - completionsW.width);
        completionsW.y = y + height;
        completionsW.calculateWidgetPositions();
    }

    /**
     * The popup below the text box. Drawn brighter than a normal background because it sits on top of one.
     */
    protected static class WCompletions extends WVerticalList {

        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            double s = GuiConstants.scale(2);
            int c = GuiConstants.color(GuiConstants.OUTLINE.get());

            Render2D.rect((float) x, (float) y, (float) width, (float) height, GuiConstants.brighter(GuiConstants.BACKGROUND.get()));

            Render2D.rect((float) x, (float) (y + height - s), (float) width, (float) s, c);
            Render2D.rect((float) x, (float) y, (float) s, (float) (height - s), c);
            Render2D.rect((float) (x + width - s), (float) y, (float) s, (float) (height - s), c);
        }

    }

    protected static class WCompletionItem extends WLabel implements ICompletionItem {

        private static final Color SELECTED_COLOR = new Color(255, 255, 255, 15);

        private boolean selected;

        public WCompletionItem(String text, boolean selected) {
            super(text);
            this.selected = selected;
        }

        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            super.onRender(context, mouseX, mouseY, delta);

            if (selected) {
                rect(SELECTED_COLOR);
            }
        }

        @Override
        public boolean isSelected() {
            return selected;
        }

        @Override
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public String getCompletion() {
            return text;
        }

    }
    
    protected double getTextWidth(int pos) {
        if (textWidths.isEmpty()) {
            return 0;
        }
        
        if (pos < 0) {
            pos = 0;
        } else if (pos >= textWidths.size()) {
            pos = textWidths.size() - 1;
        }
        
        return textWidths.getDouble(pos);
    }
    
    protected double getCursorTextWidth(int offset) {
        return getTextWidth(cursor + offset);
    }
    
    protected double getOverflowWidthForRender() {
        return textStart;
    }
    
    public String get() {
        return text;
    }
    
    public void set(String text) {
        this.text = text;
        
        cursor = MathHelper.clamp(cursor, 0, text.length());
        selectionStart = cursor;
        selectionEnd = cursor;
        
        calculateTextWidths();
        cursorChanged();
    }
    
    @Override
    public void setFocused(boolean focused) {
        if (this.focused && !focused && actionOnUnfocused != null) {
            actionOnUnfocused.run();
        }
        
        boolean wasJustFocused = focused && !this.focused;
        
        this.focused = focused;
        
        resetSelection();
        
        if (wasJustFocused) {
            onCursorChanged();
        }
    }
    
    public void setCursorMax() {
        cursor = text.length();
    }
    
    public interface Renderer {

        void render(DrawContext context, double x, double y, String text, Color color);

        default List<String> getCompletions(String text, int position) {
            return null;
        }

    }
    
    public interface ICompletionItem {
        
        boolean isSelected();
        
        void setSelected(boolean selected);
        
        String getCompletion();
        
    }
    
}
