package com.booksaw.betterTeams.commands.team.chunks;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ViewChunkCommand extends TeamSubCommand {

    @Override
    public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

        Player player = teamPlayer.getPlayer().getPlayer();

        if(team.hasViewMode(player)) {
            team.removeViewMode(player);
        } else {
            team.addViewMode(player);
        }
        return new CommandResponse(true, "chunks.view.toggled");
    }

    @Override
    public String getCommand() {
        return "view";
    }

    @Override
    public String getNode() {
        return "chunks.view";
    }

    @Override
    public String getHelp() {
        return "View claimed chunks";
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