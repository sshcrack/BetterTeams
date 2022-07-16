package com.booksaw.betterTeams;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class ItemRecipes {
    public static ArrayList<String> farmlandLore = new ArrayList<>();
    public static ArrayList<String> blockBreakPart = new ArrayList<>();
    public static ArrayList<String> blockBreakLore = new ArrayList<>();
    public static ArrayList<String> blockPlaceLore = new ArrayList<>();
    public static ArrayList<String> blockPlacePart = new ArrayList<>();

    public static ItemStack blockPlacePartItem;
    public static ItemStack blockBreakPartItem;
    public static ArrayList<NamespacedKey> namespacedKeys = new ArrayList<>();

    public ItemRecipes() {
        farmlandLore.add(ChatColor.AQUA + "Put this item into the resources chest");
        farmlandLore.add(ChatColor.AQUA + "to prevent entities from trampeling");
        farmlandLore.add(ChatColor.AQUA + "your field.");
        farmlandLore.add("");
        farmlandLore.add("");
        farmlandLore.add(ChatColor.DARK_RED + "YOU WILL LOOSE THIS BLOCK");
        farmlandLore.add(ChatColor.DARK_RED + "WHEN PLACING IT");

        blockBreakPart.add(ChatColor.AQUA + "This is just a part to craft the");
        blockBreakPart.add(ChatColor.AQUA + "block break protection upgrade");
        blockBreakPart.add("");
        blockBreakPart.add(ChatColor.GRAY + "Used to craft the Block-Break Protection upgrade");


        blockBreakLore.add(ChatColor.AQUA + "Put this item into the resources chest");
        blockBreakLore.add(ChatColor.AQUA + "to prevent enemies from destroying");
        blockBreakLore.add(ChatColor.AQUA + "your base");

        blockBreakLore.add("");
        blockBreakLore.add(ChatColor.YELLOW + "This upgrade increases the redstone");
        blockBreakLore.add(ChatColor.YELLOW + "cost by 1.5.");

        blockBreakLore.add("");
        blockBreakLore.add(ChatColor.GREEN + "Has a 10 minute PVP cooldown");


        blockPlacePart.add(ChatColor.AQUA + "This is just a part to craft the");
        blockPlacePart.add(ChatColor.AQUA + "block place protection upgrade");
        blockPlacePart.add("");
        blockPlacePart.add(ChatColor.GRAY + "Used to craft the Block-Place Protection upgrade");


        blockPlaceLore.add(ChatColor.AQUA + "Put this item into the resources chest");
        blockPlaceLore.add(ChatColor.AQUA + "to prevent enemies from placing blocks");
        blockPlaceLore.add(ChatColor.AQUA + "in your base");

        blockPlaceLore.add("");
        blockPlaceLore.add(ChatColor.YELLOW + "This upgrade increases the redstone");
        blockPlaceLore.add(ChatColor.YELLOW + "cost by 1.5.");

        blockPlaceLore.add("");
        blockPlaceLore.add(ChatColor.GREEN + "Has a 10 minute PVP cooldown");

        setupRecipes();
    }

    private void setupRecipes() {
        addFarmlandUpgrade();

        addBlockBreakPart();
        addBlockBreakUpgrade();

        addBlockPlacePart();
        addBlockPlaceUpgrade();
    }

    private void addFarmlandUpgrade() {
        ItemStack item = new ItemStack(Material.WARPED_WART_BLOCK);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Farmland" + ChatColor.YELLOW + " Chunk upgrade");

        item.setItemMeta(meta);

        item.setLore(farmlandLore);


        NamespacedKey key = new NamespacedKey(Main.plugin, "farmland_chunk_upgrade");

        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape("WCW", "CDC", "WCW");

        recipe.setIngredient('W', Material.WARPED_WART_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND_HOE);
        recipe.setIngredient('C', Material.COARSE_DIRT);



        Bukkit.addRecipe(recipe);
        namespacedKeys.add(key);
    }

    private void addBlockBreakPart() {
        ItemStack item = new ItemStack(Material.FIRE_CHARGE);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Block-Break-Part");

        item.setItemMeta(meta);

        item.setLore(blockBreakPart);

        blockBreakPartItem = item;
        NamespacedKey key = new NamespacedKey(Main.plugin, "block_break_part");

        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape("NPN",
                     "PCP",
                     "NPN");

        recipe.setIngredient('N', Material.NETHERITE_SCRAP);
        recipe.setIngredient('C', Material.CRYING_OBSIDIAN);
        recipe.setIngredient('P', Material.DIAMOND_PICKAXE);

        Bukkit.addRecipe(recipe);
        namespacedKeys.add(key);
    }

    private void addBlockBreakUpgrade() {
        ItemStack part = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta partMeta = part.getItemMeta();

        partMeta.setDisplayName(ChatColor.GREEN + "Block-Part");
        partMeta.setLore(blockBreakPart);

        part.setItemMeta(partMeta);


        ItemStack item = new ItemStack(Material.BEACON);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Block Break Protection Upgrade");

        item.setItemMeta(meta);

        item.setLore(blockBreakLore);


        NamespacedKey key = new NamespacedKey(Main.plugin, "block_break_protection_upgrade");

        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape("PTP",
                     "TBT",
                     "PTP");

        recipe.setIngredient('B', Material.BEACON);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        recipe.setIngredient('P', blockBreakPartItem);

        Bukkit.addRecipe(recipe);
        namespacedKeys.add(key);
    }




    private void addBlockPlacePart() {
        ItemStack item = new ItemStack(Material.POPPED_CHORUS_FRUIT);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + "Block-Place-Part");

        item.setItemMeta(meta);

        item.setLore(blockPlacePart);

        blockPlacePartItem = item;
        NamespacedKey key = new NamespacedKey(Main.plugin, "block_place_part");

        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape(
                "NSN",
                "SDS",
                "NSN");

        recipe.setIngredient('N', Material.NETHERITE_SCRAP);
        recipe.setIngredient('D', Material.DRAGON_BREATH);
        recipe.setIngredient('S', Material.DIAMOND_SHOVEL);

        Bukkit.addRecipe(recipe);
        namespacedKeys.add(key);
    }

    private void addBlockPlaceUpgrade() {
        ItemStack item = new ItemStack(Material.LODESTONE);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Block Place Protection Upgrade");

        item.setItemMeta(meta);

        item.setLore(blockPlaceLore);


        NamespacedKey key = new NamespacedKey(Main.plugin, "block_place_protection_upgrade");

        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape("PDP",
                "DHD",
                "PDP");

        recipe.setIngredient('H', Material.DRAGON_HEAD);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('P', blockPlacePartItem);

        Bukkit.addRecipe(recipe);
        namespacedKeys.add(key);
    }
}
