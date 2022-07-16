package com.booksaw.betterTeams;

import com.booksaw.betterTeams.customEvents.PrePurgeEvent;
import com.booksaw.betterTeams.events.MCTeamManagement.BelowNameType;
import com.booksaw.betterTeams.message.MessageManager;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;

/**
 * This class is used to manage a team and all of it's participants
 *
 * @author booksaw
 */
public class Team {

    /**
     * This is used to track if the score has been changed for any users
     */
    public static boolean scoreChanges = true;

    /**
     * This is used to track if there have been any changes to the teams balance
     */
    public static boolean moneyChanges = true;

    /**
     * Used to store all active teams
     */
    private static HashMap<UUID, Team> teamList = new HashMap<>();

    /**
     * Used to get the team with the provided ID
     *
     * @param ID the ID of the team
     * @return the team with that ID [null - the team does not exist]
     */
    @Nullable
    public static Team getTeam(UUID ID) {
        return teamList.get(ID);
    }

    private static boolean logChat;

    /**
     * Used to get the team by it's display name
     *
     * @param name the display name of the team
     * @return the team with that display name [null - no team could be found]
     */
    @Nullable
    public static Team getTeam(String name) {
        for (Entry<UUID, Team> temp : teamList.entrySet()) {
            if (temp.getValue().getName().equalsIgnoreCase(name)) {
                return temp.getValue();
            }
        }

        // trying to get team by a player name
        OfflinePlayer player = Bukkit.getPlayer(name);
        if (player == null) {
            return null;
        }
        return getTeam(player);

    }

    /**
     * Used to find the team that a specified player is in, this is the highest time
     * complexity search to find a team (O(n^2)) so only use when the other provided
     * methods are not possible
     *
     * @param player the player which is in a team
     * @return the team they are in [null - they are not in a team]
     */
    @Nullable
    public static Team getTeam(OfflinePlayer player) {
        for (Entry<UUID, Team> temp : teamList.entrySet()) {
            for (TeamPlayer teamPlayer : temp.getValue().getMembers()) {
                if (teamPlayer.getPlayer().getUniqueId().compareTo(player.getUniqueId()) == 0) {
                    return temp.getValue();
                }
            }
        }
        return null;
    }

    /**
     * This method is used at the start of the program to load all the teams into
     * memory
     */
    public static void loadTeams() {

        logChat = Main.plugin.getConfig().getBoolean("logTeamChat");
        teamList = new HashMap<>();
        for (String IDString : Main.plugin.getTeams().getStringList("teams")) {
            UUID ID = UUID.fromString(IDString);
            teamList.put(ID, new Team(ID));
        }

    }

    /**
     * This method is used to create a new team with the specified name
     * <p>
     * Checks are not carried out to ensure that the name is available, so that
     * should be done before this method is called
     * </p>
     *
     * @param name  the name of the new team
     * @param owner the owner of the new team (the player who ran /team create)
     */
    public static void createNewTeam(String name, Player owner) {

        UUID ID = UUID.randomUUID();
        // ensuring the ID is unique
        while (getTeam(ID) != null) {
            ID = UUID.randomUUID();
        }
        teamList.put(ID, new Team(name, ID, owner));

        // updating the list of teams
        List<String> teams = Main.plugin.getTeams().getStringList("teams");
        teams.add(ID.toString());
        Main.plugin.getTeams().set("teams", teams);

        Main.plugin.saveTeams();

        if (Main.plugin.teamManagement != null) {
            Main.plugin.teamManagement.displayBelowName(owner);
        }
    }

    /**
     * @return a list of all teams
     */
    public static HashMap<UUID, Team> getTeamList() {
        return teamList;
    }

    /**
     * Used for legacy support, instead use sortTeamsByScore() or
     * sortTeamsByBalance()
     *
     * @return a sorted list by the team score
     * @deprecated THIS IS ONLY FOR LEGACY SUPPORT, DO NOT USE
     */
    @Deprecated
    public static Team[] sortTeams() {
        return sortTeamsByScore();
    }

    /**
     * This method is used to sort all the teams into an arry ranking from hightest
     * score to lowest
     *
     * @return the array of teams in order of their rank
     */
    public static Team[] sortTeamsByScore() {
        Team[] rankedTeams = new Team[teamList.size()];

        int count = teamList.size() - 1;
        // adding them to a list to sort
        for (Entry<UUID, Team> team : teamList.entrySet()) {
            if (team.getValue().getTeamRank() == -1) {
                rankedTeams[count--] = team.getValue();
            } else {
                if (team.getValue().getTeamRank() >= count || rankedTeams[team.getValue().getTeamRank()] != null) {
                    rankedTeams[count--] = team.getValue();
                } else {
                    rankedTeams[team.getValue().getTeamRank()] = team.getValue();
                }
            }
        }

        for (int i = 0; i < rankedTeams.length - 1; i++) {
            boolean changes = false;

            for (int j = 0; j < rankedTeams.length - i - 1; j++) {
                if (rankedTeams[j + 1] == null) {
                    continue;
                }

                if (rankedTeams[j].getScore() < rankedTeams[j + 1].getScore()) {
                    Team temp = rankedTeams[j];
                    rankedTeams[j] = rankedTeams[j + 1];
                    rankedTeams[j + 1] = temp;
                    changes = true;
                }
            }

            if (!changes) {
                break;
            }
        }

        for (int i = 0; i < rankedTeams.length; i++) {
            rankedTeams[i].setTeamRank(i);
        }
        scoreChanges = false;
        return rankedTeams;
    }

    public static Team[] sortTeamsByBalance() {
        Team[] rankedTeams = new Team[teamList.size()];

        int count = teamList.size() - 1;
        // adding them to a list to sort
        for (Entry<UUID, Team> team : teamList.entrySet()) {
            if (team.getValue().getTeamBalRank() == -1) {
                rankedTeams[count--] = team.getValue();
            } else {
                if (team.getValue().getTeamBalRank() >= count
                        || rankedTeams[team.getValue().getTeamBalRank()] != null) {
                    rankedTeams[count--] = team.getValue();
                } else {
                    rankedTeams[team.getValue().getTeamBalRank()] = team.getValue();
                }
            }
        }

        for (int i = 0; i < rankedTeams.length - 1; i++) {
            boolean changes = false;

            for (int j = 0; j < rankedTeams.length - i - 1; j++) {
                if (rankedTeams[j].getMoney() < rankedTeams[j + 1].getMoney()) {
                    Team temp = rankedTeams[j];
                    rankedTeams[j] = rankedTeams[j + 1];
                    rankedTeams[j + 1] = temp;
                    changes = true;
                }
            }

            if (!changes) {
                break;
            }
        }

        for (int i = 0; i < rankedTeams.length; i++) {
            rankedTeams[i].setTeamBalRank(i);
        }
        moneyChanges = false;
        return rankedTeams;
    }

    public static boolean purge() {
        // calling custom bukkit event
        PrePurgeEvent event = new PrePurgeEvent();
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        for (Entry<UUID, Team> temp : Team.getTeamList().entrySet()) {
            temp.getValue().setScore(0);
        }
        System.out.println("purging team score");
        return true;
    }

    /**
     * Used to get the team which is claiming the chunk given returns null
     * if that chunk is not claimed
     *
     * @param chunk the location of the chest - must already be normalised
     * @return The team which has claimed that chest
     */
    public static Team getClamingTeam(Chunk chunk) {
        for (Object obj : teamList.values().stream().map(e -> e.getName()).toArray()) {
        }
        for (Entry<UUID, Team> team : teamList.entrySet()) {
            if (team.getValue().isClaimed(chunk)) {
                return team.getValue();
            }
        }
        return null;

    }

    public static boolean isValidTeamName(String name) {
        for (String temp : Main.plugin.getConfig().getStringList("blacklist")) {
            if (temp.equalsIgnoreCase(name)) {
                return false;
            }
        }

        for (char temp : Main.plugin.getConfig().getString("bannedChars").toCharArray()) {
            if (name.contains(Character.toString(temp))) {
                return false;
            }
        }

        if (!(name.equals(ChatColor.stripColor(name)))) {
            return false;
        }

        // stop players inputting color codes
        if (name.contains("&")) {
            return false;
        }

        String allowed = Main.plugin.getConfig().getString("allowedChars");

        if (allowed.length() != 0) {
            for (char temp : name.toCharArray()) {
                if (!allowed.contains(Character.toString(temp))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * The ID of the team (this is a unique identifier of the team which will never
     * change)
     */
    UUID ID;

    /**
     * The name of the team, this can be changed after the creation of a teams, so
     * do not store references to it
     */
    private String name;

    /**
     * The description of a team, used in /team info
     */
    private String description;

    /**
     * If the team is open or invite only
     */
    private boolean open;

    /**
     * The location of the teams home (/team home)
     */
    private Location teamHome = null;

    /**
     * This is a list of invited players to this team since the last restart of the
     * server
     */
    private final List<UUID> invitedPlayers = new ArrayList<>();

    private final List<TeamPlayer> members;
    private final List<UUID> bannedPlayers;

    /**
     * The score for the team
     */
    private int score;

    /**
     * The money that the team has
     */
    private double money;

    public boolean pvp = false;

    ChatColor color;
    /**
     * the rank of the team
     */
    private transient int rank, balRank;

    /**
     * Used to track the allies of this team
     */
    private final List<UUID> allies;

    /**
     * Used to track which teams have requested to be allies with this team
     */
    private final List<UUID> requests;

    private final Inventory echest;

    private int level;

    private String tag;

    HashMap<String, Warp> warps;

    /**
     * Resource Chest Location
     */
    Location resourceChest;
    TextLine timeText;
    TextLine upgradeText;
    long timeLeft;

    /**
     * Upgrades
     */
    boolean farmlandUpgrade;
    boolean blockBreakProtection;
    boolean blockPlaceProtection;

    /**
     * PVP Cooldown
     */
    Calendar lastPVP;

    /**
     * this is used to load a team from the configuration file
     *
     * @param ID the ID of the team to load
     */
    public Team(UUID ID) {

        this.ID = ID;
        FileConfiguration config = Main.plugin.getTeams();

        name = getString(config, "name");
        description = getString(config, "description");
        open = getBoolean(config, "open");
        score = getInteger(config, "score");
        money = getDouble(config, "money");
        String colorStr = getString(config, "color");
        if (colorStr == null) {
            this.color = ChatColor.GOLD;
            setValue(Main.plugin.getTeams(), "color", color.getChar());
            Main.plugin.saveTeams();
        } else {
            color = ChatColor.getByChar(getString(config, "color").charAt(0));
        }
        members = new ArrayList<>();
        for (String string : getStringList(config, "players")) {
            members.add(new TeamPlayer(string));
        }

        bannedPlayers = new ArrayList<>();
        for (String string : getStringList(config, "bans")) {
            bannedPlayers.add(UUID.fromString(string));
        }

        String teamHomeStr = getString(config, "home");
        if (teamHomeStr != null && !teamHomeStr.equals("")) {
            teamHome = getLocation(teamHomeStr);
        }

        allies = new ArrayList<>();
        for (String string : getStringList(config, "allies")) {
            allies.add(UUID.fromString(string));
        }

        requests = new ArrayList<>();
        for (String string : getStringList(config, "allyrequests")) {
            requests.add(UUID.fromString(string));
        }

        warps = new HashMap<>();
        for (String str : getStringList(config, "warps")) {
            String[] split = str.split(";");
            warps.put(split[0], new Warp(split));
        }


        /*
          CUSTOM PART CHUNKS, RESOURCES
         */
        String timeLeftStr = getString(config, "timeLeft");
        if (timeLeftStr != null) {
            timeLeft = Long.parseLong(timeLeftStr);
        } else {
            timeLeft = Calendar.getInstance().getTimeInMillis();
        }


        String chestStr = getString(config, "resources");
        if (chestStr == null) chestStr = "null";
        if (chestStr.equalsIgnoreCase("null")) resourceChest = null;
        else resourceChest = getLocation(chestStr);
        if (resourceChest == null) {
            timeText = null;
            upgradeText = null;
        } else {
            Location loc = resourceChest.clone();
            loc.add(.5, 1.5, .5);
            Hologram holo = HologramsAPI.createHologram(Main.plugin, loc);

            TextLine line = holo.appendTextLine("Loading...");
            TextLine upgrade = holo.appendTextLine("Loading...");

            Tools.updateHolo(line, getTimeLeft());

            this.timeText = line;
            this.upgradeText = upgrade;
        }

        List<String> temp = getStringList(config, "chunks");
        chunks = new HashMap<>();
        for (String element : temp) {
            try {
                String[] split = element.split(";");
                if (split.length < 2) continue;

                Long key = Long.parseLong(split[0]);
                String name = split[1];

                chunks.put(key, name);
            } catch (NumberFormatException ignored) {
            }
        }

        farmlandUpgrade = getBoolean(config, "farmlandUpgrade");
        blockBreakProtection = getBoolean(config, "blockBreakProtection");
        blockPlaceProtection = getBoolean(config, "blockPlaceProtection");

        if(upgradeText != null) Tools.upgradeHolo(upgradeText, this);

        /**
         * --------------------- E N D ----------------------------
         */


        echest = Bukkit.createInventory(null, 27, MessageManager.getMessage("echest.echest"));
        for (int i = 0; i < 27; i++) {
            ItemStack is = getItemStack(config, "echest." + i);
            if (is != null) {
                echest.setItem(i, is);
            }
        }

        level = getInteger(config, "level");
        if (level < 1) {
            level = 1;
        }

        tag = getString(config, "tag");

        rank = -1;
        viewMode = new ArrayList<>();

    }

    /**
     * Creates a new team with the provided name
     * <p>
     * This is a private method as the creation of a new team should be done by the
     * Team.createNewTeam(name) method
     * </p>
     *
     * @param name  The selected name for the team
     * @param ID    The UUID of the team
     * @param owner The owner of the team (whoever initiated the creation of the
     *              team)
     */
    private Team(String name, UUID ID, Player owner) {
        this.ID = ID;

        FileConfiguration config = Main.plugin.getTeams();
        setValue(config, "name", name);
        setValue(config, "description", "");
        this.name = name;
        this.description = "";
        setValue(config, "open", false);
        open = false;
        setValue(config, "score", 0);
        score = 0;
        setValue(config, "money", 0);
        money = 0;
        setValue(config, "home", "");
        rank = -1;

        color = ChatColor.getByChar(Main.plugin.getConfig().getString("defaultColor").charAt(0));
        setValue(config, "color", color.getChar());
        allies = new ArrayList<>();
        setValue(config, "allies", allies);
        requests = new ArrayList<>();
        setValue(config, "allyrequests", requests);

        warps = new HashMap<>();
        setValue(config, "warps", new ArrayList<>());

        /**
         * CUSTOM PART - CHUNKS, RESOURCES
         */

        chunks = new HashMap<>();
        setValue(config, "chunks", new ArrayList<String>());

        resourceChest = null;
        setValue(config, "resources", "null");

        timeText = null;

        timeLeft = Calendar.getInstance().getTimeInMillis();
        setValue(config, "timeLeft", timeLeft);

        farmlandUpgrade = false;
        setValue(config, "farmlandUpgrade", false);

        blockBreakProtection = false;
        setValue(config, "blockBreakProtection", false);

        blockPlaceProtection = false;
        setValue(config, "blockPlaceProtection", false);
        /**
         * ------------------- E N D ------------------)
         */

        members = new ArrayList<>();
        members.add(new TeamPlayer(owner, PlayerRank.OWNER));
        savePlayers(config);
        bannedPlayers = new ArrayList<>();
        echest = Bukkit.createInventory(null, 27, MessageManager.getMessage("echest.echest"));
        level = 1;
        setValue(config, "level", 1);
        tag = "";
        setValue(config, "tag", "");
        viewMode = new ArrayList<>();

        /*
         * do not need to save config as createNewTeam saves the config after more
         * settings modified
         */
    }

    /**
     * Used to load a string attribute from file, this is used instead of the direct
     * call to ensure any changes to the config reading system can be updated
     * quickly
     *
     * @param config    the config which stores the team's information
     * @param attribute the reference that the value is stored under
     * @return the loaded value
     */
    private String getString(FileConfiguration config, String attribute) {
        return config.getString("team." + ID + "." + attribute);
    }

    /**
     * Used to load a boolean attribute from file, this is used instead of the
     * direct call to ensure any changes to the config reading system can be updated
     * quickly
     *
     * @param config    the config which stores the team's information
     * @param attribute the reference that the value is stored under
     * @return the loaded value
     */
    private boolean getBoolean(FileConfiguration config, String attribute) {
        return config.getBoolean("team." + ID + "." + attribute);
    }

    /**
     * Used to load a integer attribute from file, this is used instead of the
     * direct call to ensure any changes to the config reading system can be updated
     * quickly
     *
     * @param config    the config which stores the team's information
     * @param attribute the reference that the value is stored under
     * @return the loaded value
     */
    private int getInteger(FileConfiguration config, String attribute) {
        return config.getInt("team." + ID + "." + attribute);
    }

    /**
     * Used to load a double attribute from file, this is used instead of the direct
     * call to ensure any changes to the config reading system can be updated
     * quickly
     *
     * @param config    the config which stores the team's information
     * @param attribute the reference that the value is stored under
     * @return the loaded value
     */
    private double getDouble(FileConfiguration config, String attribute) {
        return config.getDouble("team." + ID + "." + attribute);
    }

    /**
     * Used to load a string list from file, this is used instead of the direct call
     * to ensure any changes to the config reading system can be updated quickly
     *
     * @param config    the config which stores the team's information
     * @param attribute the reference that the value is stored under
     * @return the loaded value
     */
    private List<String> getStringList(FileConfiguration config, String attribute) {
        return config.getStringList("team." + ID + "." + attribute);
    }

    private ItemStack getItemStack(FileConfiguration config, String attribute) {
        return config.getItemStack("team." + ID + "." + attribute);
    }

    /**
     * Used to store a value to file, this is used instead of the direct call to
     * ensure any changes to the config reading system can be updated quickly
     *
     * @param config    the configuration to save the value to
     * @param attribute the attribute to save it under
     * @param value     the value of the attribute
     */
    private void setValue(FileConfiguration config, String attribute, Object value) {
        config.set("team." + ID + "." + attribute, value);
        Main.plugin.saveTeams();
    }

    /**
     * Used to get the current name of the team
     *
     * @return the name of the team
     */
    public String getName() {
        return name;
    }

    /**
     * Used to get the current name of the team
     *
     * @param resetTo the color to return to at the end of the string
     * @return the name of the team
     */
    public String getDisplayName(ChatColor resetTo) {
        if (resetTo == null) {
            return getName();
        }
        if (Main.plugin.getConfig().getBoolean("colorTeamName") && color != null) {
            return color + name + resetTo;
        }
        return name;
    }

    public String getDisplayName() {
        if (Main.plugin.getConfig().getBoolean("colorTeamName") && color != null) {
            return color + name;
        }
        return name;
    }

    public String getTag() {
        if (tag == null || tag.length() == 0) {
            return getDisplayName();
        }
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
        setValue(Main.plugin.getTeams(), "tag", tag);
        Main.plugin.saveTeams();

        if (Main.plugin.teamManagement != null) {

            if (team != null) {
                for (TeamPlayer p : members) {
                    if (p.getPlayer().isOnline()) {
                        team.removeEntry(p.getPlayer().getName());
                    }
                }
                team.unregister();
            }

            team = null;

            for (TeamPlayer p : members) {
                if (p.getPlayer().isOnline()) {
                    Main.plugin.teamManagement.displayBelowName(p.getPlayer().getPlayer());
                }
            }
        }
    }

    /**
     * Returns if the team is open
     *
     * @return [true - anyone can join the team] [false - the team is invite only]
     */
    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        setValue(Main.plugin.getTeams(), "open", open);
        Main.plugin.saveTeams();
    }

    /**
     * @return the location of the team's home
     */
    public Location getTeamHome() {
        return teamHome;
    }

    /**
     * @return The description of the team
     */
    public String getDescription() {
        return description;
    }

    /**
     * Used to change the team description
     *
     * @param description the new team description
     */
    public void setDescription(String description) {
        this.description = description;
        setValue(Main.plugin.getTeams(), "description", description);
        Main.plugin.saveTeams();
    }

    /**
     * @return The color of the team
     */
    public ChatColor getColor() {
        return color;
    }

    /**
     * Used to change the team color
     *
     * @param color the new team color
     */
    public void setColor(ChatColor color) {
        this.color = color;
        setValue(Main.plugin.getTeams(), "color", color.getChar());
        Main.plugin.saveTeams();

        if (Main.plugin.teamManagement != null) {

            if (team != null) {
                for (TeamPlayer p : members) {
                    if (p.getPlayer().isOnline()) {
                        team.removeEntry(p.getPlayer().getName());
                    }
                }
                team.unregister();
            }

            team = null;

            for (TeamPlayer p : members) {
                if (p.getPlayer().isOnline()) {
                    Main.plugin.teamManagement.displayBelowName(p.getPlayer().getPlayer());
                }
            }
        }
    }

    public List<TeamPlayer> getMembers() {
        return members;
    }

    /**
     * Used to save the members list to the configuration file
     *
     * @param config the configuration file to store the members list to
     */
    private void savePlayers(FileConfiguration config) {

        List<String> output = new ArrayList<>();

        for (TeamPlayer player : members) {
            output.add(player.toString());
        }

        setValue(config, "players", output);

    }

    /**
     * Used to save the bans list to the configuration file
     *
     * @param config the configuration file to store the ban list to
     */
    private void saveBans(FileConfiguration config) {

        List<String> output = new ArrayList<>();

        for (UUID uuid : bannedPlayers) {
            output.add(uuid.toString());
        }

        setValue(config, "bans", output);

    }

    /**
     * Used to remove the given player from the team, you must firstly be sure that
     * the player is in this team (as it is not checked or caught in this method)
     *
     * @param p the player to remove from the team
     */
    public void removePlayer(OfflinePlayer p) {

        if (Main.plugin.teamManagement != null && p.isOnline()) {
            Main.plugin.teamManagement.remove(p.getPlayer());
        }

        members.remove(getTeamPlayer(p));
        savePlayers(Main.plugin.getTeams());
        Main.plugin.saveTeams();
    }

    /**
     * Used to remove the given teamPlayer from the team, you must firstly be sure
     * that the player is in this team (as it is not checked or caught in this
     * method)
     *
     * @param p the player to remove from the team
     */
    public void removePlayer(TeamPlayer p) {
        members.remove(p);
        savePlayers(Main.plugin.getTeams());
        Main.plugin.saveTeams();

        if (team != null && p.getPlayer().isOnline()) {
            Main.plugin.teamManagement.remove(p.getPlayer().getPlayer());
        }
    }

    /**
     * Used to get the teamPlayer version of an included player
     *
     * @param player the player to search for
     * @return the team player object for that player [null - player is not in the
     * team]
     */
    @Nullable
    public TeamPlayer getTeamPlayer(OfflinePlayer player) {
        for (TeamPlayer teamPlayer : members) {
            if (teamPlayer.getPlayer().getUniqueId().compareTo(player.getUniqueId()) == 0) {
                return teamPlayer;
            }
        }
        return null;
    }

    /**
     * Used to get all players which have the specified rank within the team
     *
     * @param rank the rank to search for
     * @return a list of players which have that rank [emtpy list - no players have
     * that rank]
     */
    public List<TeamPlayer> getRank(PlayerRank rank) {
        List<TeamPlayer> toReturn = new ArrayList<>();

        for (TeamPlayer player : members) {
            if (player.getRank() == rank) {
                toReturn.add(player);
            }
        }

        return toReturn;
    }

    /**
     * This command is used to disband a team, BE CAREFUL, this is irreversible
     */
    public void disband() {

        for (UUID ally : allies) {
            Team team = Team.getTeam(ally);
            team.removeAlly(getID());
        }

        for (Entry<UUID, Team> team : teamList.entrySet()) {
            if (team.getValue().hasRequested(getID())) {
                team.getValue().removeAllyRequest(getID());
            }
        }

        // removing it from the team list, the java GC will handle the reset
        teamList.remove(ID);

        // updating the list of teams
        List<String> teams = Main.plugin.getTeams().getStringList("teams");
        teams.remove(ID.toString());
        Main.plugin.getTeams().set("teams", teams);
        Main.plugin.getTeams().set("team." + ID, null);

        Main.plugin.saveTeams();

        if (Main.plugin.teamManagement != null) {

            for (TeamPlayer p : members) {
                if (p.getPlayer().isOnline()) {
                    Main.plugin.teamManagement.remove(p.getPlayer().getPlayer());
                }
            }

            if (team != null)
                team.unregister();
            team = null;

        }
    }

    /**
     * Used to check if a player is invited to this team
     *
     * @param uuid the UUID of the player to check
     * @return [true - the player is invited] [false - the player is not invited]
     */
    public boolean isInvited(UUID uuid) {
        for (UUID temp : invitedPlayers) {
            if (temp.compareTo(uuid) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Used to create an invite for the included player to this team
     *
     * @param uniqueId the UUID of the player being invited
     */
    public void invite(UUID uniqueId) {
        invitedPlayers.add(uniqueId);

    }

    /**
     * This is used when a player is joining the team
     *
     * @param p the player who is joining the team
     */
    public void join(Player p) {

        invitedPlayers.remove(p.getUniqueId());

        for (TeamPlayer player : members) {
            if (player.getPlayer().isOnline()) {
                MessageManager.sendMessageF((CommandSender) player.getPlayer().getPlayer(), "join.notify",
                        p.getDisplayName());
            }
        }

        members.add(new TeamPlayer(p, PlayerRank.DEFAULT));
        savePlayers(Main.plugin.getTeams());
        Main.plugin.saveTeams();

        if (Main.plugin.teamManagement != null) {
            Main.plugin.teamManagement.displayBelowName(p);
        }

    }

    /**
     * This is used to set the name of the team, it is important that you check that
     * the name is unique before running this method
     *
     * @param name the new team name
     */
    public void setName(String name) {
        this.name = name;
        setValue(Main.plugin.getTeams(), "name", name);
        Main.plugin.saveTeams();

        if (Main.plugin.teamManagement != null) {

            if (team != null) {
                for (TeamPlayer p : members) {
                    if (p.getPlayer().isOnline()) {
                        team.removeEntry(p.getPlayer().getName());
                    }
                }
                team.unregister();
            }

            team = null;

            for (TeamPlayer p : members) {
                if (p.getPlayer().isOnline()) {
                    Main.plugin.teamManagement.displayBelowName(p.getPlayer().getPlayer());
                }
            }
        }
    }

    /**
     * This method is used to promote a player to the next applicable rank, this
     * method does not check the promotion is valid but instead only promotes the
     * player, see PromoteCommand to see validation
     *
     * @param promotePlayer the player to be promoted
     */
    public void promotePlayer(TeamPlayer promotePlayer) {
        if (promotePlayer.getRank() == PlayerRank.DEFAULT) {
            promotePlayer.setRank(PlayerRank.ADMIN);
        } else {
            promotePlayer.setRank(PlayerRank.OWNER);
        }

        savePlayers(Main.plugin.getTeams());
        Main.plugin.saveTeams();
    }

    /**
     * This method is used to demote a player to the next applicable rank, this
     * method does not check the demotion is valid but instead only promotes the
     * player, see DemoteCommand to see validation
     *
     * @param demotePlayer the player to be demoted
     */
    public void demotePlayer(TeamPlayer demotePlayer) {
        if (demotePlayer.getRank() == PlayerRank.OWNER) {
            demotePlayer.setRank(PlayerRank.ADMIN);
        } else {
            demotePlayer.setRank(PlayerRank.DEFAULT);
        }

        savePlayers(Main.plugin.getTeams());
        Main.plugin.saveTeams();
    }

    public void setTeamHome(Location teamHome) {
        this.teamHome = teamHome;
        setValue(Main.plugin.getTeams(), "home", getString(teamHome));
        Main.plugin.saveTeams();
    }

    public void deleteTeamHome() {
        teamHome = null;
        setValue(Main.plugin.getTeams(), "home", "");
        Main.plugin.saveTeams();
    }

    /**
     * This method is used to convert a string into a location which can be stored
     * for later use
     *
     * @param loc the string to convert into a location
     * @return the location which that string reference
     */
    public static Location getLocation(String loc) {
        String[] split = loc.split(":");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]),
                Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
    }

    /**
     * This method is used to convert a location into a string which can be stored
     * in a configuration file
     *
     * @param loc the location to convert into a string
     * @return the string which references that location
     */
    public static String getString(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw()
                + ":" + loc.getPitch();
    }

    public static Location normalise(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * This method is used to add a player to the list of players which are banned
     * from the team
     *
     * @param player the player to add to the list
     */
    public void banPlayer(OfflinePlayer player) {
        bannedPlayers.add(player.getUniqueId());
        saveBans(Main.plugin.getTeams());
        Main.plugin.saveTeams();
    }

    /**
     * This method is used to remove a player from the list of players which are
     * banned from the team
     *
     * @param player the player to remove from the list
     */
    public void unbanPlayer(OfflinePlayer player) {
        // used to avoid concurrent modification error
        UUID store = null;
        for (UUID uuid : bannedPlayers) {
            if (player.getUniqueId().equals(uuid)) {
                store = uuid;
            }
        }
        bannedPlayers.remove(store);

        saveBans(Main.plugin.getTeams());
        Main.plugin.saveTeams();
    }

    /**
     * This method searches the ban list to check if the player is banned
     *
     * @param player the player to check
     * @return [true - the player is banned] [false - the player isen't banned]
     */
    public boolean isBanned(OfflinePlayer player) {
        for (UUID uuid : bannedPlayers) {
            if (player.getUniqueId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Used when a player sends a message to the team chat
     *
     * @param sender  the player which sent the message to the team chat
     * @param message the message to send to the team chat
     */
    public void sendMessage(TeamPlayer sender, String message) {
        ChatColor returnTo = ChatColor.RESET;
        String toTest = MessageManager.getMessage(sender.getPlayer().getPlayer(), "chat.syntax");
        int value = toTest.indexOf("%s");
        if (value > 2) {
            for (int i = value; i >= 0; i--) {
                if (toTest.charAt(i) == '�') {
                    returnTo = ChatColor.getByChar(toTest.charAt(i + 1));
                    if (toTest != null) {
                        break;
                    }
                }
            }
        }

        String fMessage = String.format(MessageManager.getMessage("chat.syntax"),
                sender.getPrefix(returnTo) + sender.getPlayer().getPlayer().getDisplayName(), message);

        fMessage = fMessage.replace("$name$", sender.getPrefix(returnTo) + sender.getPlayer().getPlayer().getName());
        fMessage = fMessage.replace("$message$", message);

        for (TeamPlayer player : members) {
            if (player.getPlayer().isOnline()) {
                player.getPlayer().getPlayer().sendMessage(fMessage);
            }
        }

        for (CommandSender temp : Main.plugin.chatManagement.spy) {
            if (temp instanceof Player && getTeamPlayer((Player) temp) != null) {
                continue;
            }

            MessageManager.sendMessageF(temp, "spy.team", getName(), sender.getPlayer().getPlayer().getName(), message);
        }

        if (logChat) {
            Bukkit.getLogger().info("[BetterTeams]" + fMessage);
        }
    }

    /**
     * Used to send a message to all of the teams allies
     *
     * @param sender  the player who sent the message
     * @param message the message that the player sent
     */
    public void sendAllyMessage(TeamPlayer sender, String message) {
        ChatColor returnTo = ChatColor.RESET;
        String toTest = MessageManager.getMessage(sender.getPlayer().getPlayer(), "chat.syntax");
        int value = toTest.indexOf("%s");
        if (value > 2) {
            for (int i = value; i >= 0; i--) {
                if (toTest.charAt(i) == '�') {
                    returnTo = ChatColor.getByChar(toTest.charAt(i + 1));
                    if (toTest != null) {
                        break;
                    }
                }
            }
        }

        String fMessage = String.format(MessageManager.getMessage("allychat.syntax"), getName(),
                sender.getPrefix(returnTo) + sender.getPlayer().getPlayer().getDisplayName(), message);

        fMessage = fMessage.replace("$name$", sender.getPrefix(returnTo) + sender.getPlayer().getPlayer().getName());
        fMessage = fMessage.replace("$message$", message);

        for (TeamPlayer player : members) {
            if (player.getPlayer().isOnline()) {
                player.getPlayer().getPlayer().sendMessage(fMessage);
            }
        }

        for (UUID ally : allies) {
            Team temp = Team.getTeam(ally);
            for (TeamPlayer player : temp.getMembers()) {
                if (player.getPlayer().isOnline()) {
                    player.getPlayer().getPlayer().sendMessage(fMessage);
                }
            }
        }

        for (CommandSender temp : Main.plugin.chatManagement.spy) {
            if (temp instanceof Player) {
                Team spyTeam = Team.getTeam((Player) temp);
                // if they are receiving the message without chat spy
                if (spyTeam == this || (spyTeam != null && isAlly(spyTeam.getID()))) {
                    continue;
                }
            }
            MessageManager.sendMessageF(temp, "spy.ally", getName(), sender.getPlayer().getName(), message);
        }

        if (logChat) {
            Bukkit.getLogger().info("[BetterTeams]" + fMessage);
        }
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
        scoreChanges = true;
        setValue(Main.plugin.getTeams(), "score", score);
        Main.plugin.saveTeams();
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
        moneyChanges = true;
        setValue(Main.plugin.getTeams(), "money", money);
        Main.plugin.saveTeams();
    }

    /**
     * @return the rank of the team (-1 if the team has not been ranked)
     */
    public int getTeamRank() {
        return rank;
    }

    public void setTeamRank(int rank) {
        this.rank = rank;
    }

    /**
     * Set resource chest location
     */
    public void setResourceLoc(Location loc) {
        FileConfiguration config = Main.plugin.getTeams();
        this.resourceChest = loc;
        if (loc == null) setValue(config, "resources", "null");
        else setValue(config, "resources", getString(loc));

        Main.plugin.saveTeams();
    }

    public void setTimeText(TextLine holo) {
        if(holo != null) Tools.upgradeHolo(holo, this);
        this.timeText = holo;
    }
    public void setUpgradeText(TextLine holo) {
        if(holo != null) Tools.upgradeHolo(holo, this);
        this.upgradeText = holo;
    }

    public void setTimeLeft(Calendar calendar) {
        this.timeLeft = calendar.getTimeInMillis();
        setValue(Main.plugin.getTeams(), "timeLeft", timeLeft);
        Main.plugin.saveTeams();
    }

    public void setFarmlandUpgrade(boolean upgrade) {
        this.farmlandUpgrade = upgrade;

        setValue(Main.plugin.getTeams(), "farmlandUpgrade", upgrade);
        Main.plugin.saveTeams();
    }

    public void setBlockBreakProtection(boolean protection) {
        this.blockBreakProtection = protection;

        setValue(Main.plugin.getTeams(), "blockBreakProtection", protection);
        Main.plugin.saveTeams();
    }

    public void setBlockPlaceProtection(boolean protection) {
        this.blockPlaceProtection = protection;

        setValue(Main.plugin.getTeams(), "blockPlaceProtection", protection);
        Main.plugin.saveTeams();
    }

    public void setLastPVP(Calendar pvp) {
        setLastPVP(pvp, true);
    }

    public void setLastPVP(Calendar pvp, boolean setAllies) {
        if(setAllies) {
            allies.forEach(uuid -> {
                Team team = getTeam(uuid);
                if(team == null) return;

                team.setLastPVP(pvp, false);
            });
        }

        this.lastPVP = pvp;
    }

    public Calendar getTimeLeft() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(this.timeLeft));
        return calendar;
    }

    public Duration getDuration() {
        Calendar left = this.getTimeLeft();
        return Duration.between(Calendar.getInstance().toInstant(), left.toInstant());
    }

    public Calendar getLastPVP() {
        return this.lastPVP;
    }

    public boolean inCooldown() {
        Calendar lastPVP = this.getLastPVP();
        if(lastPVP == null) return false;

        Duration duration = Duration.between(Calendar.getInstance().toInstant(), lastPVP.toInstant());
        return duration.getSeconds() <= 60 * 10; // 1 Minute
    }

    public Location getResourceLoc() {
        return this.resourceChest;
    }

    public TextLine getTimeText() {
        return this.timeText;
    }
    public TextLine getUpgradeText() {
        return this.upgradeText;
    }

    public boolean getFarmlandUpgrade() {
        return this.farmlandUpgrade;
    }

    public boolean getBlockBreakProtection() {
        return this.blockBreakProtection;
    }
    public boolean getBlockPlaceProtection() {
        return this.blockPlaceProtection;
    }

    private void setTeamBalRank(int rank) {
        this.balRank = rank;
    }

    public int getTeamBalRank() {
        return balRank;
    }

    org.bukkit.scoreboard.Team team;

    /**
     * Used throughout all below name management (showing team name above player
     * name)
     *
     * @param board the scoreboard to add the team to
     * @param type  the type of the scoreboard team (mainly for prefix / suffix)
     * @return the team that has been created
     */
    public org.bukkit.scoreboard.Team getScoreboardTeam(Scoreboard board, BelowNameType type) {
        if (team != null) {
            return team;
        }

        String name = String.format(color + MessageManager.getMessage("nametag.syntax"), getTag());
        int attempt = 0;
        do {
            try {
                String attemptStr = ((attempt > 0) ? attempt + "" : "");
                String teamName = getName();

                while (teamName.length() + attemptStr.length() > 16) {
                    teamName = teamName.substring(0, teamName.length() - 1);
                }

                if (board.getTeam(teamName + attemptStr) != null) {
                    team = null;
                    attempt++;
                    continue;
                }
                team = board.registerNewTeam(teamName + attemptStr);

            } catch (Exception e) {
                team = null;
                attempt++;
            }

        } while (team == null && attempt < 100);

        if (team == null) {
            Bukkit.getLogger().warning(
                    "An avaliable team cannot be found, be prepared for a lot of errors. (this should never happen, and should always be reported to booksaw)");
            Bukkit.getLogger().warning("This catch is merely here to stop the server crashing");
            return null;
        }

        Main.plugin.teamManagement.setupTeam(team, name);

        return team;

    }

    /**
     * Used to return the scoreboard team, and not create a new one if it does not
     * exist
     *
     * @return The scoreboard team (if already created)
     */
    public org.bukkit.scoreboard.Team getScoreboardTeamOrNull() {
        return team;
    }

    public String getBalance() {
        DecimalFormat df = new DecimalFormat("0.00");
        df.setGroupingUsed(true);
        df.setGroupingSize(3);
        String moneyString = df.format(money);
        return moneyString;
    }

    public void setTitle(TeamPlayer player, String title) {
        player.setTitle(title);
        savePlayers(Main.plugin.getTeams());
    }

    /**
     * Used to add an ally for this team
     *
     * @param ally the UUID of the new ally
     */
    public void addAlly(UUID ally) {
        allies.add(ally);
        saveAllies();

        Team team = Team.getTeam(ally);
        // notifying all online members of the team
        for (TeamPlayer p : members) {
            OfflinePlayer player = p.getPlayer();
            if (player.isOnline()) {
                MessageManager.sendMessageF(player.getPlayer(), "ally.ally", team.getDisplayName());
            }
        }
    }

    /**
     * Used to remove an ally from this team
     *
     * @param ally the ally to remove
     */
    public void removeAlly(UUID ally) {
        allies.remove(ally);
        saveAllies();
    }

    /**
     * Used to check if a team is in alliance with this team
     *
     * @param team the team to check for allies
     * @return if the team is an ally
     */
    public boolean isAlly(UUID team) {
        return allies.contains(team);
    }

    /**
     * Used to add an ally request to this team
     *
     * @param team the team that has sent the request
     */
    public void addAllyRequest(UUID team) {
        requests.add(team);
        saveAllyRequests();

        Team t = Team.getTeam(team);
        // notifying all online owners of the team
        for (TeamPlayer p : members) {
            OfflinePlayer player = p.getPlayer();
            if (player.isOnline() && p.getRank() == PlayerRank.OWNER) {
                MessageManager.sendMessageF(player.getPlayer(), "ally.request", t.getDisplayName());
            }
        }
    }

    /**
     * Used to remove an ally request from this team
     *
     * @param team the team to remove the ally request for
     */
    public void removeAllyRequest(UUID team) {
        requests.remove(team);
        saveAllyRequests();
    }

    /**
     * Used to check if a team has sent an ally request for this team
     *
     * @param team the team to check for
     * @return if they have sent an ally request
     */
    public boolean hasRequested(UUID team) {
        return requests.contains(team);
    }

    /**
     * @return the list of all UUIDS of teams that have sent ally requests
     */
    public List<UUID> getRequests() {
        return requests;
    }

    /**
     * @return the list of all UUIDS of teams that are allied with this team
     */
    public List<UUID> getAllies() {
        return allies;
    }

    /**
     * Used to save the list of allies for this team
     */
    private void saveAllies() {
        List<String> toSave = new ArrayList<>();

        for (UUID uuid : allies) {
            toSave.add(uuid.toString());
        }

        setValue(Main.plugin.getTeams(), "allies", toSave);
        Main.plugin.saveTeams();
    }

    /**
     * Used to save the list of requests for allies for this team
     */
    private void saveAllyRequests() {
        List<String> toSave = new ArrayList<>();

        for (UUID uuid : requests) {
            toSave.add(uuid.toString());
        }

        setValue(Main.plugin.getTeams(), "allyrequests", toSave);
        Main.plugin.saveTeams();
    }

    public UUID getID() {
        return ID;
    }

    /**
     * Used to check if a member of this team can damage the specified player
     *
     * @param player the player to check for
     * @return if this team can damage that player
     */
    public boolean canDamage(Player player) {
        Team team = Team.getTeam(player);
        if (team == null) {
            return true;
        }
        return canDamage(team);
    }

    /**
     * Used to check if this team can damage members of the specified team
     *
     * @param team the team to test
     * @return if players of this team can damage members of the other team
     */
    public boolean canDamage(Team team) {
        if (team.isAlly(getID()) || team == this) {
            if (pvp && team.pvp) {
                return true;
            }
            return false;
        }
        return true;
    }

    public boolean hasMaxAllies() {
        int limit = Main.plugin.getConfig().getInt("allyLimit");
        if (limit == -1) {
            return false;
        }

        return allies.size() >= limit;
    }

    /**
     * Used to save all warps that this team has set
     */
    public void saveWarps() {
        List<String> toSave = new ArrayList<>();

        for (Entry<String, Warp> temp : warps.entrySet()) {
            toSave.add(temp.getValue().toString());
        }

        setValue(Main.plugin.getTeams(), "warps", toSave);
    }

    /**
     * Used to get a warp with the specified name
     *
     * @param name the name of the warp
     * @return the warp with that name
     */
    public Warp getWarp(String name) {
        return warps.get(name);
    }

    public void addWarp(Warp warp) {
        warps.put(warp.getName(), warp);
        saveWarps();
    }

    public void delWarp(String name) {
        warps.remove(name);
        saveWarps();
    }

    public HashMap<String, Warp> getWarps() {
        return warps;
    }

    /**
     * Used to get a list of all the online players that are on this team
     *
     * @return a list of online members for this team
     */
    public List<Player> getOnlineMemebers() {
        List<Player> online = new ArrayList<>();

        for (TeamPlayer player : members) {
            if (player.getPlayer().isOnline()) {
                online.add(player.getPlayer().getPlayer());
            }
        }

        return online;
    }


    // CHUNK CLAIM COMPONENT
    private HashMap<Long, String> chunks;

    private List<UUID> viewMode;

    public void addViewMode(Player player) {
        viewMode.add(player.getUniqueId());
    }

    public void removeViewMode(Player player) {
        viewMode.remove(player.getUniqueId());
    }

    public boolean hasViewMode(Player player) {
        return viewMode.contains(player.getUniqueId());
    }

    public boolean anyoneViewMode() {
        return viewMode.size() != 0;
    }

    public ArrayList<Player> getViewModes() {
        ArrayList<Player> temp = new ArrayList<>();
        viewMode.forEach(uuid -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.isOnline()) {
                temp.add(offline.getPlayer());
            }
        });
        return temp;
    }

    /**
     * Used to claim a chunk
     *
     * @param chunk The chunk to claim
     */
    public void addClaim(Chunk chunk) {
        chunks.put(chunk.getChunkKey(), chunk.getWorld().getName());
        saveClaims();
    }

    public List<Chunk> getChunks() {
        List<Chunk> fetched = new ArrayList<>();

        if (chunks.size() == 0) return fetched;
        for (Long key : chunks.keySet()) {
            String name = chunks.get(key);

            World world = Bukkit.getWorld(name);
            if (world == null) {
                Bukkit.getLogger().warning("World not found. Removing claimed chunk " + key.toString());
                chunks.remove(key);
                saveClaims();
                continue;
            }

            fetched.add(world.getChunkAt(key));
        }
        return fetched;
    }

    /**
     * Used to unclaim a chunk
     */
    public void removeClaim(Chunk chunk) {
        chunks.remove(chunk.getChunkKey());
    }

    public void clearClaims() {
        chunks = new HashMap<>();
        saveClaims();
    }

    public int getClaimCount() {
        return chunks.size();
    }

    public boolean isClaimed(Chunk chunk) {
        boolean claimed = false;
        World chunkWorld = chunk.getWorld();
        for (Entry<Long, String> entry : chunks.entrySet()) {
            Long key = entry.getKey();
            String world = entry.getValue();
            if (chunk.getChunkKey() == key && world.equals(chunkWorld.getName())) {
                claimed = true;
            }
        }
        return claimed;
    }

    private void saveClaims() {
        List<String> toSave = new ArrayList<>();
        chunks.forEach((Long chunk, String world) -> {
            toSave.add(chunk.toString() + ";" + world);
        });
        setValue(Main.plugin.getTeams(), "chunks", toSave);
        Main.plugin.saveTeams();
    }

    public void saveEchest() {
        for (int i = 0; i < 27; i++) {
            ItemStack is = echest.getItem(i);
            if (is != null) {
                setValue(Main.plugin.getTeams(), "echest." + i, is);
            } else {
                setValue(Main.plugin.getTeams(), "echest." + i, null);
            }
        }
        Main.plugin.saveTeams();
    }

    public Inventory getEchest() {
        return echest;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        setValue(Main.plugin.teams, "level", level);
    }

}
