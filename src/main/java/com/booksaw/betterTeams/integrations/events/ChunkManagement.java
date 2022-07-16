package com.booksaw.betterTeams.events;

import com.booksaw.betterTeams.ItemRecipes;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.Tools;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ChunkManagement implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.getChunk().getChunkKey() == to.getChunk().getChunkKey()) return;
        Team oldTeam = Team.getClamingTeam(from.getChunk());
        Team newTeam = Team.getClamingTeam(to.getChunk());

        if (newTeam != null && oldTeam != null) {
            if (!newTeam.getName().equals(oldTeam.getName())) {
                player.sendTitle("", ChatColor.GRAY + "You have entered " + newTeam.getDisplayName() + "'s" + ChatColor.GRAY + " area", 5, 20, 5);
                return;
            }
        }

        if (oldTeam == null && newTeam != null) {
            player.sendTitle("", ChatColor.GRAY + "You have entered " + newTeam.getDisplayName() + "'s" + ChatColor.GRAY + " area", 5, 20, 5);
            return;
        }

        if (newTeam == null && oldTeam != null) {
            player.sendTitle("", ChatColor.GRAY + "You have left " + oldTeam.getDisplayName() + "'s" + ChatColor.GRAY + " area", 5, 30, 5);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location blockLoc = block.getLocation();
        if (block.getType() != Material.CHEST) return;

        Team team = Team.getTeam(e.getPlayer());
        Location resources = team.getResourceLoc();
        if (resources == null) return;

        World world = resources.getWorld();

        if (!Tools.locationEquals(blockLoc, resources)) return;
        TextLine line = team.getTimeText();
        if (line != null) line.getParent().delete();

        TextLine upgrade = team.getUpgradeText();
        if (upgrade != null) upgrade.getParent().delete();

        team.setResourceLoc(null);
        team.setTimeText(null);
        world.playSound(resources, Sound.BLOCK_BEACON_DEACTIVATE, 100, 0);
        e.getPlayer().sendMessage(ChatColor.RED + "Resource-Chest deleted.");
    }

    @EventHandler
    public void onInventoryClosed(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();

        if (!(e.getPlayer() instanceof Player)) return;
        if (inv.getLocation() == null) return;

        Player player = (Player) e.getPlayer();
        Team team = Team.getTeam(player);
        if (team == null) return;

        Location chestLoc = team.getResourceLoc();
        Calendar timeLeft = team.getTimeLeft();

        if (chestLoc == null || timeLeft == null) return;
        if (!Tools.locationEquals(chestLoc, inv.getLocation())) return;


        Block chestBlock = chestLoc.getBlock();
        if (chestBlock.getType() != Material.CHEST) return;

        ItemStack[] contents = inv.getStorageContents();
        contents = checkRedstone(contents, player, team);
        contents = checkFarmupgrade(contents, team);
        contents = checkBlockBreakUpgrade(contents, team);
        contents = checkBlockPlaceUpgrade(contents, team);

        Tools.upgradeHolo(team.getUpgradeText(), team);
        inv.setContents(contents);
    }

    public ItemStack[] checkFarmupgrade(ItemStack[] contents, Team team) {
        Location chestLoc = team.getResourceLoc();

        boolean upgrade = team.getFarmlandUpgrade();
        if (upgrade) return contents;

        Stream<ItemStack> stream = Arrays.stream(contents);
        Stream<ItemStack> notNull = stream.filter(Objects::nonNull);
        String[] mainLores = ItemRecipes.farmlandLore.toArray(new String[0]);

        AtomicBoolean subtracted = new AtomicBoolean(false);
        notNull.forEach(item -> {
            if (item.getLore() == null) return;
            String[] itemLores = item.getLore().toArray(new String[0]);

            boolean isItem = item.getType() == Material.WARPED_WART_BLOCK && Arrays.equals(itemLores, mainLores);
            if (isItem && !subtracted.get()) {
                item.setAmount(item.getAmount() - 1);
                subtracted.set(true);
            }
        });

        if (subtracted.get()) {
            team.setFarmlandUpgrade(true);
            spawnFarmUpgradeParticles(chestLoc);
        }
        return contents;
    }

    public ItemStack[] checkBlockPlaceUpgrade(ItemStack[] contents, Team team) {
        Location chestLoc = team.getResourceLoc();

        boolean upgrade = team.getBlockPlaceProtection();
        if (upgrade) return contents;

        Stream<ItemStack> stream = Arrays.stream(contents);
        Stream<ItemStack> notNull = stream.filter(Objects::nonNull);
        String[] craftingLores = ItemRecipes.blockPlaceLore.toArray(new String[0]);

        AtomicBoolean subtracted = new AtomicBoolean(false);
        notNull.forEach(item -> {
            if (item.getLore() == null) return;
            String[] itemLores = item.getLore().toArray(new String[0]);

            boolean isItem = item.getType() == Material.LODESTONE && Arrays.equals(itemLores, craftingLores);
            if (isItem && !subtracted.get()) {
                item.setAmount(item.getAmount() - 1);
                subtracted.set(true);
            }
        });

        if (subtracted.get()) {
            team.setBlockPlaceProtection(true);
            spawnBlockUpgradeParticles(chestLoc);
        }
        return contents;
    }

    public ItemStack[] checkBlockBreakUpgrade(ItemStack[] contents, Team team) {
        Location chestLoc = team.getResourceLoc();

        boolean upgrade = team.getBlockBreakProtection();
        if (upgrade) return contents;

        Stream<ItemStack> stream = Arrays.stream(contents);
        Stream<ItemStack> notNull = stream.filter(Objects::nonNull);
        String[] craftingLores = ItemRecipes.blockBreakLore.toArray(new String[0]);

        AtomicBoolean subtracted = new AtomicBoolean(false);
        notNull.forEach(item -> {
            if (item.getLore() == null) return;
            String[] itemLores = item.getLore().toArray(new String[0]);

            boolean isItem = item.getType() == Material.BEACON && Arrays.equals(itemLores, craftingLores);
            if (isItem && !subtracted.get()) {
                item.setAmount(item.getAmount() - 1);
                subtracted.set(true);
            }
        });

        if (subtracted.get()) {
            team.setBlockBreakProtection(true);
            spawnBlockUpgradeParticles(chestLoc);
        }
        return contents;
    }

    public ItemStack[] checkRedstone(ItemStack[] contents, Player player, Team team) {
        Location chestLoc = team.getResourceLoc();
        Calendar timeLeft = team.getTimeLeft();
        Calendar now = Calendar.getInstance();

        Duration duration = Duration.between(now.toInstant(), timeLeft.toInstant());
        if (duration.getSeconds() <= 0) timeLeft = now;

        ArrayList<ItemStack> newContents = new ArrayList<>();
        int redstoneCount = 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType() != Material.REDSTONE) {
                newContents.add(item);
                continue;
            }

            redstoneCount += item.getAmount();
            newContents.add(null);
        }

        ItemStack[] newContentArray = contents;
        if (redstoneCount > 0) {
            newContentArray = newContents.toArray(new ItemStack[0]);
            spawnSuccessParticles(chestLoc);

            int addTime = Main.plugin.getConfig().getInt("addTime");

            double costs = addTime * redstoneCount * (team.getBlockBreakProtection() ? 0.75 : 1);
            timeLeft.add(Calendar.SECOND, Math.toIntExact(Math.round(costs)));

            team.setTimeLeft(timeLeft);
            Tools.updateHolo(team.getTimeText(), timeLeft);

            player.sendMessage(ChatColor.GREEN + "Time added!");
        }

        return newContentArray;
    }

    public void spawnSuccessParticles(Location chestLoc) {
        World world = chestLoc.getWorld();

        Location particleLoc = chestLoc.clone();
        particleLoc.add(.5, 1, .5);

        world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 100, .25, .25, .25);
        world.playSound(particleLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, 10, 0);
    }

    public void spawnFarmUpgradeParticles(Location chestLoc) {
        World world = chestLoc.getWorld();

        Location particleLoc = chestLoc.clone();
        particleLoc.add(.5, 1, .5);

        world.spawnParticle(Particle.CRIMSON_SPORE, particleLoc, 400, .25, .25, .25);
        world.playSound(particleLoc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1, 0);
    }

    public void spawnBlockUpgradeParticles(Location chestLoc) {
        World world = chestLoc.getWorld();

        Location particleLoc = chestLoc.clone();
        particleLoc.add(.5, 1, .5);

        world.spawnParticle(Particle.EXPLOSION_HUGE, particleLoc, 10, .25, .25, .25);
        world.spawnParticle(Particle.WHITE_ASH, particleLoc, 400, .25, .25, .25);
        world.playSound(particleLoc, Sound.ENTITY_WITHER_SPAWN, 1, 0);
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent e) {
        Inventory source = e.getSource();
        Inventory dest = e.getDestination();
        ItemStack item = e.getItem();

        Location sourceLoc = source.getLocation();
        Location destLoc = dest.getLocation();
        if (sourceLoc == null || destLoc == null) return;

        Team team = Team.getClamingTeam(destLoc.getChunk());
        if (team == null) return;

        Location resourceChest = team.getResourceLoc();

        if (resourceChest == null) return;
        if (!Tools.locationEquals(resourceChest, destLoc)) return;
        if (item.getType() != Material.REDSTONE) return;


        Calendar timeLeft = team.getTimeLeft();
        Calendar now = Calendar.getInstance();

        Duration duration = Duration.between(now.toInstant(), timeLeft.toInstant());
        if (duration.getSeconds() <= 0) timeLeft = now;

        int addTime = Main.plugin.getConfig().getInt("addTime");
        int amount = item.getAmount();
        timeLeft.add(Calendar.SECOND, amount * addTime);

        team.setTimeLeft(timeLeft);
        item.setType(Material.AIR);
        item.setAmount(0);
        e.setItem(item);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Action action = e.getAction();
        Block block = e.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();
        Chunk chunk = loc.getChunk();

        if (action != Action.PHYSICAL || block.getType() != Material.FARMLAND)
            return;

        Team team = Team.getClamingTeam(chunk);
        if (team == null) return;
        if (!team.getFarmlandUpgrade()) return;

        Duration duration = team.getDuration();
        if (duration.getSeconds() < 0) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Material to = e.getTo();
        Block block = e.getBlock();

        Location loc = block.getLocation();
        Chunk chunk = loc.getChunk();

        if (to != Material.DIRT || block.getType() != Material.FARMLAND)
            return;

        Team team = Team.getClamingTeam(chunk);
        if (team == null) return;
        if (!team.getFarmlandUpgrade()) return;

        Duration duration = team.getDuration();
        if (duration.getSeconds() < 0) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onExplosion(ExplosionPrimeEvent e) {
        Entity entity = e.getEntity();
        Location loc = entity.getLocation();


        int radius = Math.round(e.getRadius());
        ArrayList<Chunk> chunks = Tools.getNearbyChunks(loc, radius);

        ArrayList<Team> claimedTeams = new ArrayList<>();
        for (Chunk chunk : chunks) {
            Team got = Team.getClamingTeam(chunk);
            if (got != null) claimedTeams.add(got);
        }

        AtomicBoolean isProtected = new AtomicBoolean(false);
        claimedTeams.forEach(team -> {
            Duration duration = team.getDuration();
            if (team.getBlockBreakProtection() && duration.getSeconds() > 0 && !team.inCooldown())
                isProtected.set(true);
        });

        if (isProtected.get()) {
            e.setRadius(0);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.getPlayer().discoverRecipes(ItemRecipes.namespacedKeys);
    }

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Team claiming = Team.getClamingTeam(e.getBlock().getChunk());
        if(claiming == null) return;

        if (!claiming.getBlockPlaceProtection() || claiming.getDuration().getSeconds() < 0) return;
        if (claiming.inCooldown()) return;

        Team playerTeam = Team.getTeam(player);
        if(playerTeam != null)
            if(claiming.getID() == playerTeam.getID()) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity damageTaker = e.getEntity();

        if (!(damageTaker instanceof Player)) return;
        if (!(damager instanceof Player)) return;

        Player player = (Player) damager;
        Player taker = (Player) damageTaker;

        Calendar now = Calendar.getInstance();
        Team takerTeam = Team.getTeam(taker);
        Team team = Team.getTeam(player);

        if (takerTeam != null) takerTeam.setLastPVP(now);
        if (team != null) team.setLastPVP(now);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        Bukkit.getLogger().info("Yield " + e.getYield() + " Entity " + e.getEntity().getType().toString());
        Entity entity = e.getEntity();
        Location loc = entity.getLocation();


        int radius = 8; //Radius is max 7.5
        ArrayList<Chunk> chunks = Tools.getNearbyChunks(loc, radius);

        ArrayList<Team> claimedTeams = new ArrayList<>();
        for (Chunk chunk : chunks) {
            Team got = Team.getClamingTeam(chunk);
            if (got != null) claimedTeams.add(got);
        }

        AtomicBoolean isProtected = new AtomicBoolean(false);
        claimedTeams.forEach(team -> {
            Duration duration = team.getDuration();
            if (team.getBlockBreakProtection() && duration.getSeconds() > 0 && !team.inCooldown())
                isProtected.set(true);
        });


        if (isProtected.get()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        if(e.getBlock().getType() == Material.AIR) {
            int radius = 11;
            ArrayList<Chunk> chunks = Tools.getNearbyChunks(e.getBlock().getLocation(), radius);

            ArrayList<Team> claimedTeams = new ArrayList<>();
            for (Chunk chunk : chunks) {
                Team got = Team.getClamingTeam(chunk);
                if (got != null) claimedTeams.add(got);
            }

            AtomicBoolean isProtected = new AtomicBoolean(false);
            claimedTeams.forEach(team -> {
                Duration duration = team.getDuration();
                if (team.getBlockBreakProtection() && duration.getSeconds() > 0 && !team.inCooldown())
                    isProtected.set(true);
            });


            if (isProtected.get()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Team claiming = Team.getClamingTeam(e.getBlock().getChunk());
        if(claiming == null) return;

        if (!claiming.getBlockPlaceProtection() || claiming.getDuration().getSeconds() < 0) return;
        if (claiming.inCooldown()) return;

        Team playerTeam = Team.getTeam(player);
        if(playerTeam != null)
            if(claiming.getID() == playerTeam.getID()) return;
        e.setCancelled(true);
    }
}
