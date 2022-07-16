package com.booksaw.betterTeams.commands.team.chunks;

import com.booksaw.betterTeams.*;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class ResourcesChunkCommand extends TeamSubCommand {

    @Override
    public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

        if (teamPlayer.getRank() == PlayerRank.DEFAULT) {
            return new CommandResponse("needAdmin");
        }

        Player player = teamPlayer.getPlayer().getPlayer();
        Location loc = player.getLocation();

        Block block = loc.getBlock();
        if(block.getType() != Material.CHEST)
            return new CommandResponse("chunks.resources.nochest");

        BlockState state = block.getState();
        if (!(state instanceof Chest))
            return new CommandResponse("chunks.resources.nochest");

        boolean claimed = team.isClaimed(loc.getChunk());
        if(!claimed)
            return new CommandResponse("chunks.resources.notclaimed");

        Chest chest = (Chest) state;
        Inventory inv = chest.getInventory();
        ItemStack[] contents = inv.getContents();

        HashMap<Material, Integer> needed = new HashMap<>();
        needed.put(Material.OBSIDIAN, 4);
        needed.put(Material.DIAMOND, 4);
        needed.put(Material.ENDER_EYE, 1);

        HashMap<Material, Integer> inChest = (HashMap<Material, Integer>) needed.clone();
        inChest.keySet().forEach(key -> inChest.put(key, 0));

        for (ItemStack item : contents) {
            if(item == null) continue;
            Material type = item.getType();

            if(inChest.containsKey(type)) {
                int current = inChest.get(type);
                current += item.getAmount();

                inChest.put(type, current);
            }
        }

        ArrayList<Material> has = new ArrayList<>();
        inChest.forEach((key, amount) -> {
            int neededAmount = needed.get(key);
            if(neededAmount <= amount) {
                has.add(key);
            }
        });

        if(has.size() < inChest.size()) {
            player.sendMessage("Not enough Materials");
            return new CommandResponse("chunks.resources.nomaterials");
        }

        ArrayList<ItemStack> filtered = new ArrayList<>();
        for (ItemStack item : contents) {
            if(item == null) continue;

            Material type = item.getType();
            int itemAmount = item.getAmount();
            if(!needed.containsKey(type)) continue;

            int neededAmount = needed.get(type);
            if(itemAmount < neededAmount) {
                neededAmount -= itemAmount;
                itemAmount = 0;
            }
            if(itemAmount > neededAmount) {
                itemAmount -= neededAmount;
                neededAmount = 0;
            }
            if(itemAmount == neededAmount) {
                itemAmount = 0;
                neededAmount = 0;
            }

            item.setAmount(itemAmount);
            if(itemAmount != 0) {
                filtered.add(item);
            }

            needed.put(type, neededAmount);
        }

        ItemStack[] convertTo = new ItemStack[filtered.size()];
        inv.setContents(filtered.toArray(convertTo));
        World world = player.getWorld();
        Location chestLoc = chest.getLocation();
        chestLoc.add(0.5, 0, 0.5);

        world.strikeLightningEffect(chestLoc);
        world.strikeLightningEffect(chestLoc);
        world.strikeLightningEffect(chestLoc);
        world.spawnParticle(Particle.EXPLOSION_NORMAL, chestLoc, 1);
        for(int i = 0; i < 50; i++) {
            world.spawnParticle(Particle.TOTEM, chestLoc, 100);
            world.spawnParticle(Particle.SMOKE_NORMAL, chestLoc, 100);
        }

        team.setResourceLoc(chest.getLocation());


        Calendar timeLeft = Calendar.getInstance();

        timeLeft.add(Calendar.MINUTE, 10);
        team.setTimeLeft(timeLeft);

        Location holoLoc = chest.getBlock().getLocation().clone();
        holoLoc.add(.5, 1.5, .5);

        Hologram holo = HologramsAPI.createHologram(Main.plugin, holoLoc);

        TextLine timeText = holo.appendTextLine("Loading...");
        TextLine upgradeText = holo.appendTextLine("Loading...");

        Tools.updateHolo(timeText, timeLeft);
        Tools.upgradeHolo(upgradeText, team);

        team.setTimeText(timeText);
        team.setUpgradeText(upgradeText);

        return new CommandResponse(true, "chunks.resources.success");
    }

    @Override
    public String getCommand() {
        return "resources";
    }

    @Override
    public String getNode() {
        return "chunks.resources";
    }

    @Override
    public String getHelp() {
        return "Set the chest which contains the resources";
    }

    @Override
    public String getArguments() {
        return "";
    }

    @Override
    public int getMinimumArguments() {
        return 0;
    }

    @Override
    public int getMaximumArguments() {
        return 0;
    }

    @Override
    public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
    }

}
