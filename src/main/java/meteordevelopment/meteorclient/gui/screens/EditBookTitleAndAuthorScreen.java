/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EditBookTitleAndAuthorScreen extends WindowScreen {
    
    private final ItemStack itemStack;
    private final Hand hand;
    
    public EditBookTitleAndAuthorScreen(ItemStack itemStack, Hand hand) {
        super("Edit title & author");
        this.itemStack = itemStack;
        this.hand = hand;
    }
    
    @Override
    public void initWidgets() {
        WTable table = add(new WTable()).expandX().widget();
        
        table.add(new WLabel("Title"));
        WTextBox title = table.add(new WTextBox(itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).title().get(mc.shouldFilterText()))).minWidth(220).expandX().widget();
        table.row();
        
        table.add(new WLabel("Author"));
        WTextBox author = table.add(new WTextBox(itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).author())).minWidth(220).expandX().widget();
        table.row();
        
        table.add(new WButton("Done")).expandX().widget().action = () -> {
            WrittenBookContentComponent component = itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
            WrittenBookContentComponent newComponent = new WrittenBookContentComponent(RawFilteredPair.of(title.get()), author.get(), component.generation(), component.pages(), component.resolved());
            itemStack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, newComponent);
            
            BookScreen.Contents contents = new BookScreen.Contents(itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).getPages(mc.shouldFilterText()));
            List<String> pages = new ArrayList<>(contents.getPageCount());
            for (int i = 0; i < contents.getPageCount(); i++) {
                pages.add(contents.getPage(i).getString());
            }
            
            mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(hand == Hand.MAIN_HAND ? mc.player.getInventory().getSelectedSlot() : 40, pages, Optional.of(title.get())));
            
            close();
        };
    }
    
}
