package com.booksaw.betterTeams;

import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class Tools {
    public static String formatDuration(Duration duration) {
        double sec_num = duration.getSeconds();
        if(sec_num < 0) return "00:00:00";

        int hours   = (int) Math.floor(sec_num / 3600);
        int minutes = (int) Math.floor((sec_num - (hours * 3600)) / 60);
        int seconds = (int) sec_num - (hours * 3600) - (minutes * 60);

        String returnHours = Integer.toString(hours);
        String returnMinutes = Integer.toString(minutes);
        String returnSeconds = Integer.toString(seconds);

        if (hours   < 10)
            returnHours = "0"+hours;

        if (minutes < 10)
            returnMinutes = "0"+minutes;

        if (seconds < 10)
            returnSeconds = "0"+seconds;

        return returnHours+':'+returnMinutes+':'+returnSeconds;
    }

    public static void upgradeHolo(TextLine line, Team team) {
        boolean farmUpgrade = team.getFarmlandUpgrade();
        boolean breakUpgrade = team.getBlockBreakProtection();
        boolean placeUpgrade = team.getBlockPlaceProtection();

        HashMap<String, Boolean> map = new HashMap<>();

        map.put("Farmland", farmUpgrade);
        map.put("Break", breakUpgrade);
        map.put("Place", placeUpgrade);

        String text = ChatColor.GREEN + "Upgrades: ";
        ArrayList<String> upgrades = new ArrayList<>();
        map.forEach((key, value1) -> {
            boolean value = value1;

            if (value) upgrades.add(key);
        });

        if(upgrades.size() == 0)
            text += ChatColor.RED + "none";
        else {
            text += String.join(", ", upgrades);
        }

        line.setText(text);
    }

    public static void updateHolo(TextLine textLine, Calendar timeLeft) {
        Calendar now = Calendar.getInstance();

        Calendar added = Calendar.getInstance();
        added.add(Calendar.MINUTE, 10);

        Duration duration = Duration.between(now.toInstant(), timeLeft.toInstant());
        if(duration.getSeconds() < 0)
            textLine.setText(ChatColor.RED + "No resources left.");
        else
            textLine.setText(ChatColor.AQUA + formatDuration(duration));
    }

    public static Calendar LongToCalendar(long time) {
        Calendar calendar = Calendar.getInstance();
        Bukkit.getLogger().info("Time " + time);
        calendar.setTime(new Date(time));

        return calendar;
    }

    public static boolean locationEquals(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ() &&
               loc1.getWorld().getUID() == loc2.getWorld().getUID();
    }


    public static ArrayList<Chunk> getNearbyChunks(Location location, int radius) {
        HashMap<Long,Chunk> chunks = new HashMap();
        for(int x = -radius; x <= radius; x++) {
            int z = x < 0 ?  radius + x : radius - x;

            Location blockLocPos = location.clone();
            Location blockLocNeg = location.clone();

            blockLocPos.add(x, 0, z);
            blockLocNeg.add(-x, 0, -z);

            Block pos = blockLocPos.getBlock();
            Chunk posChunk = pos.getChunk();

            Block neg = blockLocNeg.getBlock();
            Chunk negChunk = neg.getChunk();

            chunks.put(negChunk.getChunkKey(), negChunk);
            chunks.put(posChunk.getChunkKey(), posChunk);
        }

        return new ArrayList<>(chunks.values());
    }
}
