package com.booksaw.betterTeams.commands.team.chunks;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnclaimChunkCommand extends TeamSubCommand {
    /**
     * This HashMap is used to track all confirm messages, to ensure that the user
     * wants to delete the resource chest
     */
    HashMap<UUID, Long> confirmation = new HashMap<>();

    @Override
    public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

        if (teamPlayer.getRank() == PlayerRank.DEFAULT) {
            return new CommandResponse("needAdmin");
        }

        Player player = teamPlayer.getPlayer().getPlayer();
        Location loc = player.getLocation();

        Team claimedBy = Team.getClamingTeam(loc.getChunk());
        Team playerTeam = Team.getTeam(player);
        if (claimedBy == null) {
            return new CommandResponse("chunks.remove.notClaimed");
        }

        if(claimedBy.getID() != playerTeam.getID()) {
            return new CommandResponse("chunks.remove.notClaimed");
        }



        Location resourceLoc = team.getResourceLoc();
        if(resourceLoc != null) {
            Chunk resourceChunk = resourceLoc.getChunk();
            Chunk chunk = loc.getChunk();

            if(chunk.getChunkKey() == resourceChunk.getChunkKey()) {
                UUID found = null;
                // if they have already had the confirm dialogue
                for (Map.Entry<UUID, Long> temp : confirmation.entrySet()) {
                    if (temp.getKey().compareTo(teamPlayer.getPlayer().getUniqueId()) == 0
                            && temp.getValue() < System.currentTimeMillis() + 10000) {
                        found = temp.getKey();
                    }
                }

                if (found != null) {
                    confirmation.remove(found);

                    TextLine line = team.getTimeText();
                    if(line != null) line.getParent().delete();

                    TextLine upgrade = team.getUpgradeText();
                    if(upgrade != null) upgrade.getParent().delete();

                    resourceLoc.getBlock().setType(Material.AIR);
                    team.setTimeText(null);
                    team.setUpgradeText(null);
                    team.setResourceLoc(null);
                    team.removeClaim(loc.getChunk());
                    return new CommandResponse(true, "chunks.remove.success");
                }

                confirmation.put(player.getUniqueId(), System.currentTimeMillis());
                return new CommandResponse("chunks.remove.confirm");
            }
        }

        // they can unclaim the chunk
        team.removeClaim(loc.getChunk());
        return new CommandResponse(true, "chunks.remove.success");
    }

    @Override
    public String getCommand() {
        return "unclaim";
    }

    @Override
    public String getNode() {
        return "chunks.unclaim";
    }

    @Override
    public String getHelp() {
        return "Unclaim the chunk you are in";
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
