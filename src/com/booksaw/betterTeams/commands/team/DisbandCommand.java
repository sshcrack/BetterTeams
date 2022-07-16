package com.booksaw.betterTeams.commands.team;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * This class handles the /team disband command
 * 
 * @author booksaw
 *
 */
public class DisbandCommand extends TeamSubCommand {

	/**
	 * This HashMap is used to track all confirm messages, to ensure that the user
	 * wants to disband the team when they type the command
	 */
	HashMap<UUID, Long> confirmation = new HashMap<>();

	@Override
	public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

		if (teamPlayer.getRank() != PlayerRank.OWNER) {
			return new CommandResponse("needOwner");
		}

		UUID found = null;
		// if they have already had the confirm dialogue
		for (Entry<UUID, Long> temp : confirmation.entrySet()) {
			if (temp.getKey().compareTo(teamPlayer.getPlayer().getUniqueId()) == 0
					&& temp.getValue() < System.currentTimeMillis() + 10000) {
				found = temp.getKey();
			}
		}

		if (found != null) {
			Location resourceLoc = team.getResourceLoc();
			TextLine line = team.getTimeText();
			if(line != null) line.getParent().delete();

			TextLine upgrade = team.getUpgradeText();
			if(upgrade != null) upgrade.getParent().delete();


			if(resourceLoc != null)
				resourceLoc.getBlock().setType(Material.AIR);

			team.setTimeText(null);
			team.setUpgradeText(null);
			team.setResourceLoc(null);

			team.disband();
			confirmation.remove(found);

			return new CommandResponse(true, "disband.success");
		}

		confirmation.put(teamPlayer.getPlayer().getUniqueId(), System.currentTimeMillis());
		return new CommandResponse("disband.confirm");

	}

	@Override
	public String getCommand() {
		return "disband";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}
	
	@Override
	public String getNode() {
		return "disband";
	}

	@Override
	public String getHelp() {
		return "Disband your current team";
	}

	@Override
	public String getArguments() {
		return "";
	}

	@Override
	public int getMaximumArguments() {
		return 0;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
	}

}
