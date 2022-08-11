package net.zhuruoling.omms.controller.fabric.item;


import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class ItemFactory {
    public static BlockItem getJoinableServerItem(String displayName, String proxyName){
        var itemSettings = new Item.Settings();
        itemSettings.maxCount(64);
        itemSettings.group(ItemGroup.BUILDING_BLOCKS);
        return new BlockItem(Blocks.GREEN_CONCRETE,itemSettings);
    }

}
