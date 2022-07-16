package com.booksaw.betterTeams.commands.team.chunks;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ClaimChunkCommand extends TeamSubCommand {

    @Override
    public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

        if (teamPlayer.getRank() == PlayerRank.DEFAULT) {
            return new CommandResponse("needAdmin");
        }

        Player player = teamPlayer.getPlayer().getPlayer();
        Location loc = player.getLocation();

        Team claimedBy = Team.getClamingTeam(loc.getChunk());
        if (claimedBy != null) {
            Bukkit.getLogger().info(claimedBy.getName());
            return new CommandResponse("chunks.claim.claimed");
        }

        Team playerTeam = Team.getTeam(player);
        if(playerTeam.getClaimCount() >= 16) {
            return new CommandResponse("chunks.claim.limitReached");
        }

        // they can claim the chunk
        team.addClaim(loc.getChunk());
        return new CommandResponse(true, "chunks.claim.success");
    }

    @Override
    public String getCommand() {
        return "claim";
    }

    @Override
    public String getNode() {
        return "chunks.claim";
    }

    @Override
    public String getHelp() {
        return "Claim the chunk you are in";
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
