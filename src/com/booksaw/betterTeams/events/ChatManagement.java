package com.booksaw.betterTeams.events;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public class ChatManagement implements Listener {

	private static boolean doPrefix;
	public List<CommandSender> spy = new ArrayList<>();

	public static void enable() {
		doPrefix = Main.plugin.getConfig().getBoolean("prefix");
	}

	/**
	 * This detects when the player speaks and either adds a prefix or puts the
	 * message in the team chat
	 * 
	 * @param event the chat event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncPlayerChatEvent event) {

		if (event.isCancelled()) {
			return;
		}

		Player p = event.getPlayer();
		Team team = Team.getTeam(p);

		if (team == null) {
			return;
		}

		TeamPlayer teamPlayer = team.getTeamPlayer(p);

		if ((teamPlayer.isInTeamChat() || teamPlayer.isInAllyChat())
				&& (event.getMessage().startsWith("!") && event.getMessage().length() > 1)) {
			event.setMessage(event.getMessage().substring(1));
		} else if (teamPlayer.isInTeamChat()) {
			// player is sending to team chat
			event.setCancelled(true);

			team.sendMessage(teamPlayer, event.getMessage());
			// Used as some chat plugins do not accept when a message is cancelled
			event.setMessage("");
			event.setFormat("");
			return;
		} else if (teamPlayer.isInAllyChat()) {
			// player is sending to ally chat
			event.setCancelled(true);

			team.sendAllyMessage(teamPlayer, event.getMessage());
			// Used as some chat plugins do not accept when a message is cancelled
			event.setMessage("");
			event.setFormat("");
			return;
		}

		if (doPrefix) {
			String syntax = MessageManager.getMessage(p, "prefixSyntax");
			ChatColor returnTo = ChatColor.RESET;
			int value = syntax.indexOf("�");
			if (value != -1 && syntax.charAt(value) == '�') {
				returnTo = ChatColor.getByChar(syntax.charAt(value + 1));
			}

			event.setFormat(String.format(syntax, team.getDisplayName(returnTo), event.getFormat()));
//				event.setFormat(ChatColor.AQUA + "[" + team.getName() + "] " + ChatColor.WHITE + event.getFormat());
		}

	}

	@EventHandler
	public void spyQuit(PlayerQuitEvent e) {
		if (spy.contains(e.getPlayer())) {
			spy.remove(e.getPlayer());
		}
	}

}
