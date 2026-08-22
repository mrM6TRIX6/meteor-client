/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.renderer.color.Color;
import meteordevelopment.meteorclient.systems.modules.render.hud.elements.TextHUD;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import net.minecraft.client.gui.DrawContext;
import org.meteordev.starscript.utils.SemanticToken;
import org.meteordev.starscript.utils.SemanticTokenProvider;
import org.meteordev.starscript.utils.SemanticTokenType;

import java.util.ArrayList;
import java.util.List;

public class StarscriptTextBoxRenderer implements WTextBox.Renderer {

    private static final Color RED = new Color(225, 25, 25);

    private final List<SemanticToken> tokens = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();

    private String lastText;

    @Override
    public void render(DrawContext context, double x, double y, String text, Color color) {
        // Generate
        if (lastText == null || !lastText.equals(text)) {
            lastText = text;

            SemanticTokenProvider.get(text, tokens);
            convertTokensToSections();
        }

        // Render
        for (Section section : sections) {
            GuiConstants.text(section.text, x, y, section.color);
            x += GuiConstants.textWidth(section.text);
        }
    }

    @Override
    public List<String> getCompletions(String text, int position) {
        List<String> completions = new ArrayList<>();

        MeteorStarscript.ss.getCompletions(text, position, (completion, function) -> {
            completions.add(function ? completion + "(" : completion);
        });

        completions.sort(String::compareToIgnoreCase);

        return completions;
    }

    private void convertTokensToSections() {
        sections.clear();

        int start = 0;

        for (SemanticToken token : tokens) {
            if (start != token.start) {
                sections.add(new Section(
                    lastText.substring(start, token.start),
                    GuiConstants.STARSCRIPT_TEXT
                ));
            }

            String text = lastText.substring(token.start, token.end);

            sections.add(new Section(
                text,
                getColorForToken(token.type, text)
            ));

            start = token.end;
        }

        if (start < lastText.length()) {
            sections.add(new Section(
                lastText.substring(start),
                GuiConstants.STARSCRIPT_TEXT
            ));
        }
    }

    private static Color getColorForToken(SemanticTokenType type, String text) {
        return switch (type) {
            case Dot -> GuiConstants.STARSCRIPT_DOTS;
            case Comma -> GuiConstants.STARSCRIPT_COMMAS;
            case Operator -> GuiConstants.STARSCRIPT_OPERATORS;
            case String -> GuiConstants.STARSCRIPT_STRINGS;
            case Number -> GuiConstants.STARSCRIPT_NUMBERS;
            case Keyword -> GuiConstants.STARSCRIPT_KEYWORDS;
            case Paren -> GuiConstants.STARSCRIPT_PARENTHESIS;
            case Brace -> GuiConstants.STARSCRIPT_BRACES;
            case Identifier -> GuiConstants.STARSCRIPT_TEXT;
            case Map -> GuiConstants.STARSCRIPT_ACCESSED_OBJECTS;
            case Section -> {
                if (text.startsWith("#")) {
                    text = text.substring(1);
                }

                try {
                    yield TextHUD.getSectionColor(Integer.parseInt(text));
                } catch (NumberFormatException ignored) {}

                yield GuiConstants.STARSCRIPT_TEXT;
            }
            case Error -> RED;
        };
    }

    private record Section(String text, Color color) {}

}
