package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.comparators.GovernmentComparators;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinSettings;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TownyCommand extends BaseCommand implements CommandExecutor {

	// protected static TownyUniverse universe;
	private static Towny plugin;

	private static List<String> towny_top = new ArrayList<>();
	private static List<String> towny_war = new ArrayList<>();
	private static String towny_version;
	private static final List<String> townyTabCompletes = Arrays.asList(
		"map",
		"prices",
		"time",
		"top",
		"spy",
		"universe",
		"v",
		"war",
		"switches",
		"itemuse",
		"farmblocks",
		"wildsblocks",
		"plotclearblocks"
	);
	
	private static final List<String> townyConsoleTabCompletes = Arrays.asList(
		"prices",
		"time",
		"top",
		"spy",
		"universe",
		"tree",
		"v",
		"war"
	);

	private static final List<String> townyWarTabCompletes = Arrays.asList(
		"stats",
		"scores",
		"hud",
		"participants"
	);
	
	private static final List<String> townyTopTabCompletes = Arrays.asList(
		"residents",
		"land",
		"balance"
	);
	
	private static final List<String> townyTopTownNationCompletes = Arrays.asList(
		"all",
		"town",
		"nation"
	);
	
	private static final List<String> townyTopLandTabCompletes = Arrays.asList(
		"all",
		"resident",
		"town"
	);

	public TownyCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		towny_version = Colors.Green + "Towny version: " + Colors.LightGreen + plugin.getVersion();

		towny_war.add(ChatTools.formatTitle("/towny war"));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "stats", ""));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "scores", ""));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "participants [page #]", ""));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "hud", ""));

		if (sender instanceof Player) {
			if (plugin.isError()) {
				sender.sendMessage(Colors.Rose + "[Towny Error] Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			parseTownyCommand(player, args);
		} else {
			// Console output
			if (args.length == 0) {
				HelpMenu.GENERAL_HELP.send(sender);
			} else if (args[0].equalsIgnoreCase("tree")) {
				for (String line : TownyUniverse.getInstance().getTreeString(0)) {
					sender.sendMessage(line);
				}
			} else if (args[0].equalsIgnoreCase("time")) {
				TownyMessaging.sendMsg(Translation.of("msg_time_until_a_new_day") + TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime()));
			} else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("v"))
				sender.sendMessage(Colors.strip(towny_version));
			else if (args[0].equalsIgnoreCase("war")) {
				boolean war = TownyWar(StringMgmt.remFirstArg(args), null);
				if (war)
					for (String line : towny_war)
						sender.sendMessage(Colors.strip(line));
				else
					sender.sendMessage("The world isn't currently at war.");
				
			} else if (args[0].equalsIgnoreCase("universe")) {
				for (String line : getUniverseStats())
					sender.sendMessage(Colors.strip(line));
			}

		}
		towny_war.clear();
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase()) {
			case "top":
				switch (args.length) {
					case 2:
						return NameUtil.filterByStart(townyTopTabCompletes, args[1]);
					case 3:
						switch (args[1].toLowerCase()) {
							case "residents":
								return NameUtil.filterByStart(townyTopTownNationCompletes, args[2]);
							case "land":
								return NameUtil.filterByStart(townyTopLandTabCompletes, args[2]);
							case "balance":
								return NameUtil.filterByStart(townyTopTownNationCompletes, args[2]);
						}
				}
				break;
			case "war":
				if (args.length == 2)
					return NameUtil.filterByStart(townyWarTabCompletes, args[1]);
				break;
			default:
				if (args.length == 1) {
					if (sender instanceof Player) {
						return NameUtil.filterByStart(townyTabCompletes, args[0]);
					} else {
						return NameUtil.filterByStart(townyConsoleTabCompletes, args[0]);
					}
				}
		}
		
		return Collections.emptyList();
	}

	private void parseTownyCommand(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0) {
			HelpMenu.GENERAL_HELP.send(player);
			return;
		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.HELP.send(player);
			return;
		}

		try {

			if (split[0].equalsIgnoreCase("map")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_MAP.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (split.length > 1 && split[1].equalsIgnoreCase("big"))
					TownyAsciiMap.generateAndSend(plugin, player, 18);
				else
					showMap(player);
			} else if (split[0].equalsIgnoreCase("prices")) {
				Town town = null;
				if (!TownySettings.isUsingEconomy())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				if (split.length > 1) {
					town = townyUniverse.getTown(split[1]);
					
					if (town == null) {
						sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[1]));
						return;
					}
				} else {
					Optional<Resident> resOpt = TownyUniverse.getInstance().getResidentOpt(player.getUniqueId());
					
					if (resOpt.isPresent() && resOpt.get().hasTown()) {
						try {
							town = resOpt.get().getTown();
						} catch (NotRegisteredException ignore) {
						}
					}
				}

				for (String line : getTownyPrices(town))
					player.sendMessage(line);
			} else if (split[0].equalsIgnoreCase("switches")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openGUIInventory(resident, TownySettings.getSwitchMaterials(), "Towny Switch List");
			} else if (split[0].equalsIgnoreCase("itemuse")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openGUIInventory(resident, TownySettings.getItemUseMaterials(), "Towny ItemUse List");
			} else if (split[0].equalsIgnoreCase("farmblocks")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openGUIInventory(resident, TownySettings.getFarmPlotBlocks(), "Towny FarmBlocks List");
			} else if (split[0].equalsIgnoreCase("wildsblocks")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openGUIInventory(resident, TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName()).getUnclaimedZoneIgnoreMaterials(), "Towny WildsBlocks List");
			} else if (split[0].equalsIgnoreCase("plotclearblocks")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openGUIInventory(resident, TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName()).getPlotManagementMayorDelete(), "Towny Plot Clear Delete List");
			} else if (split[0].equalsIgnoreCase("top")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TOP.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				TopCommand(player, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("tree")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TREE.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				consoleUseOnly(player);
			} else if (split[0].equalsIgnoreCase("time")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TIME.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				TownyMessaging.sendMsg(player, Translation.of("msg_time_until_a_new_day") + TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime()));
			} else if (split[0].equalsIgnoreCase("universe")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_UNIVERSE.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				for (String line : getUniverseStats())
					player.sendMessage(line);
			} else if (split[0].equalsIgnoreCase("version") || split[0].equalsIgnoreCase("v")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_VERSION.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				player.sendMessage(towny_version);
			} else if (split[0].equalsIgnoreCase("war")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_WAR.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				boolean war = TownyWar(StringMgmt.remFirstArg(split), player);
				if (war)
					for (String line : towny_war)
						player.sendMessage(Colors.strip(line));
				else
					sendErrorMsg(player, "The world isn't currently at war.");

				towny_war.clear();
			} else if (split[0].equalsIgnoreCase("spy")) {
				if (townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_CHAT_SPY.getNode())) {
					Resident resident = getResidentOrThrow(player.getUniqueId());
					resident.toggleMode(split, true);
				} else
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_command_disable"));

			} else
				sendErrorMsg(player, "Invalid sub command.");

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}

	}

	private boolean TownyWar(String[] args, Player p) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (TownyAPI.getInstance().isWarTime() && args.length > 0) {
			towny_war.clear();
			if (args[0].equalsIgnoreCase("stats"))
				towny_war.addAll(townyUniverse.getWarEvent().getStats());
			else if (args[0].equalsIgnoreCase("scores"))
				towny_war.addAll(townyUniverse.getWarEvent().getScores(-1));
			else if (args[0].equalsIgnoreCase("participants")) {
				try {
					parseWarParticipants(p, args);
				} catch (NotRegisteredException ignored) {
				}
				return true;
			}
			else if (args[0].equalsIgnoreCase("hud") && p == null)
				towny_war.add("No hud for console!");
			else if (args[0].equalsIgnoreCase("hud") && p != null) {
				if (townyUniverse.getPermissionSource().testPermission(p, PermissionNodes.TOWNY_COMMAND_TOWNY_WAR_HUD.getNode())) {
					plugin.getHUDManager().toggleWarHUD(p);
				} else {
					TownyMessaging.sendErrorMsg(p, Translation.of("msg_err_command_disable"));
				}
			}
		}

		return TownyAPI.getInstance().isWarTime();
	}

	private void parseWarParticipants(Player player, String[] split) throws NotRegisteredException {
		Resident resident = getResidentOrThrow(player.getUniqueId());
		List<Town> townsToSort = War.warringTowns;
		List<Nation> nationsToSort = War.warringNations;
		int page = 1;
		List<String> output = new ArrayList<>();
		String nationLine;
		String townLine;
		for (Nation nations : nationsToSort) {
			nationLine = Colors.Gold + "-" + nations.getName();
			if (resident.hasNation())
				if (resident.getTown().getNation().hasEnemy(nations))
					nationLine += Colors.Red + " (Enemy)";
				else if (resident.getTown().getNation().hasAlly(nations))
					nationLine += Colors.Green + " (Ally)";
			output.add(nationLine);
			for (Town towns : townsToSort) {
				if (towns.getNation().equals(nations)) {
					townLine = Colors.Blue + "  -" + towns.getName();
					if (towns.isCapital())
						townLine += Colors.LightBlue + " (Capital)";
					output.add(townLine);
				}
			}
		}
		int total = (int) Math.ceil((output.size()) / (double) 10);
		if (split.length > 1) {
			try {
				page = Integer.parseInt(split[1]);
				if (page < 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative"));
					return;
				} else if (page == 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					return;
				}
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
				return;
			}
		}
		if (page > total) {
			TownyMessaging.sendErrorMsg(player, Translation.of("LIST_ERR_NOT_ENOUGH_PAGES", total));
			return;
		}

		int iMax = page * 10;
		if ((page * 10) > output.size()) {
			iMax = output.size();
		}
		List<String> warparticipantsformatted = new ArrayList<>();
		for (int i = (page - 1) * 10; i < iMax; i++) {
			String line = output.get(i);
			warparticipantsformatted.add(line);
		}
		player.sendMessage(ChatTools.formatList("War Participants",
				Colors.Gold + "Nation Name" + Colors.Gray + " - " + Colors.Blue + "Town Names",
				warparticipantsformatted, Translation.of("LIST_PAGE", page, total)
				)
		);
		output.clear();
	}	
	
	private void TopCommand(Player player, String[] args) {
		TownyUniverse universe = TownyUniverse.getInstance();
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			towny_top.add(ChatTools.formatTitle("/towny top"));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "residents [all/town/nation]", ""));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "land [all/resident/town]", ""));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "balance [all/town/nation]", ""));
		} else if (args[0].equalsIgnoreCase("residents"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<ResidentList> list = new ArrayList<>(universe.getDataSource().getTowns());
				list.addAll(universe.getDataSource().getNations());
				towny_top.add(ChatTools.formatTitle("Most Residents"));
				towny_top.addAll(getMostResidents(list));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Town"));
				towny_top.addAll(getMostResidents(new ArrayList<>(universe.getDataSource().getTowns())));
			} else if (args[1].equalsIgnoreCase("nation")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Nation"));
				towny_top.addAll(getMostResidents(new ArrayList<>(universe.getDataSource().getNations())));
			} else
				sendErrorMsg(player, "Invalid sub command.");
		else if (args[0].equalsIgnoreCase("land"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<TownBlockOwner> list = new ArrayList<>(universe.getResidents());
				list.addAll(universe.getDataSource().getTowns());
				towny_top.add(ChatTools.formatTitle("Most Land Owned"));
				towny_top.addAll(getMostLand(list));
			} else if (args[1].equalsIgnoreCase("resident")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Resident"));
				towny_top.addAll(getMostLand(new ArrayList<>(universe.getResidents())));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Town"));
				towny_top.addAll(getMostLand(new ArrayList<>(universe.getDataSource().getTowns())));
			} else
				sendErrorMsg(player, "Invalid sub command.");
		else if (args[0].equalsIgnoreCase("balance")) {
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<Government> list = new ArrayList<>();
				list.addAll(universe.getTowns());
				list.addAll(universe.getNationsMap().values());
				towny_top.add(ChatTools.formatTitle("Top Bank Balances"));
				towny_top.addAll(getTopBankBalance(list));
			} else if (args[1].equalsIgnoreCase("town")) {
				List<Government> list = new ArrayList<>(universe.getTowns());
				towny_top.add(ChatTools.formatTitle("Top Bank Balances by Town"));
				towny_top.addAll(getTopBankBalance(list));
			} else if (args[1].equalsIgnoreCase("nation")) {
				List<Government> list = new ArrayList<>(universe.getNationsMap().values());
				towny_top.add(ChatTools.formatTitle("Top Bank Balances by Nation"));
				towny_top.addAll(getTopBankBalance(list));
			} else {
				sendErrorMsg(player, "Invalid sub command.");
			}
		}
		else
			sendErrorMsg(player, "Invalid sub command.");

		for (String line : towny_top)
			player.sendMessage(line);

		towny_top.clear();

	}

	public List<String> getUniverseStats() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyDataSource townyDS = townyUniverse.getDataSource();
		List<String> output = new ArrayList<>();
		output.add(""); // Intentionally left blank
		output.add("\u00A70-\u00A74###\u00A70---\u00A74###\u00A70-   " + Colors.Gold + "[" + Colors.Yellow + "Towny " + Colors.Green + plugin.getVersion() + Colors.Gold + "]");
		output.add("\u00A74#\u00A7c###\u00A74#\u00A70-\u00A74#\u00A7c###\u00A74#\u00A70   " + Colors.Blue + Translation.of("msg_universe_attribution") + Colors.LightBlue + "Chris H (Shade), ElgarL, LlmDl");
		output.add("\u00A74#\u00A7c####\u00A74#\u00A7c####\u00A74#   " + Colors.LightBlue + Translation.of("msg_universe_contributors") + Colors.Rose + Translation.of("msg_universe_heart"));
		output.add("\u00A70-\u00A74#\u00A7c#######\u00A74#\u00A70-");
		output.add("\u00A70--\u00A74##\u00A7c###\u00A74##\u00A70--   " + Colors.Blue + Translation.of("res_list")+ ": " + Colors.LightBlue + townyUniverse.getNumResidents() + Colors.Gray + " | " + Colors.Blue + Translation.of("town_plu") + ": " + Colors.LightBlue + townyDS.getTowns().size() + Colors.Gray + " | " + Colors.Blue + Translation.of("nation_plu") + ": " + Colors.LightBlue + townyDS.getNations().size());
		output.add("\u00A70----\u00A74#\u00A7c#\u00A74#\u00A70----   " + Colors.Blue + Translation.of("world_plu") + ": " + Colors.LightBlue + townyDS.getWorlds().size() + Colors.Gray + " | " + Colors.Blue + Translation.of("townblock_plu") + ": " + Colors.LightBlue + townyUniverse.getTownBlocks().size());
		output.add("\u00A70-----\u00A74#\u00A70-----   " + Colors.LightGreen + "https://TownyAdvanced.github.io/");
		output.add(""); // Intentionally left blank
		

		// Other TownyAdvanced plugins to report versions
		int plugins = 0;
		String townyPlugins = Colors.Gold + "[";
		
		Plugin townyChat = Bukkit.getServer().getPluginManager().getPlugin("TownyChat");
		if (townyChat != null){
			townyPlugins += Colors.Yellow + "TownyChat " + Colors.Green + townyChat.getDescription().getVersion() + " ";
			plugins++;
		}
		
		Plugin townyF = Bukkit.getServer().getPluginManager().getPlugin("TownyFlight");
		if (townyF != null) {
			townyPlugins += Colors.Yellow + "TownyFlight " + Colors.Green + townyF.getDescription().getVersion() + " ";
			plugins++;
		}

		if (plugins > 0)
			output.add(townyPlugins + Colors.Gold + "]");
		return output;
	}

	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player - Player.
	 */

	public static void showMap(Player player) {

		TownyAsciiMap.generateAndSend(plugin, player, 7);
	}

	/**
	 * Returns prices for town's taxes/upkeep.
	 * @param town - The town being checked.
	 * @return - Prices screen for a town.
	 */
	public List<String> getTownyPrices(Town town) {

		List<String> output = new ArrayList<>();
		Nation nation = null;

		if (town != null)
			if (town.hasNation())
				try {
					nation = town.getNation();
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}

		output.add(ChatTools.formatTitle("Prices"));
		output.add(Colors.Yellow + "[New] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNewTownPrice()) + Colors.Gray + " | " + Colors.Green + "Nation: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNewNationPrice()));
		output.add(Colors.Yellow + "[Reclaim] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownRuinSettings.getEcoPriceReclaimTown()));
		if (town != null) {
			output.add(Colors.Yellow + "[Upkeep] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeepCost(town)) + Colors.Gray + " | " + Colors.Green + "Nation: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationUpkeepCost(nation)));
			if (town.isOverClaimed() && TownySettings.getUpkeepPenalty() > 0)
				output.add(Colors.Yellow + "[Overclaimed Upkeep] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownPenaltyUpkeepCost(town)));
			output.add(Colors.Yellow + "[Claiming] " + Colors.Green + "TownBlock: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(town.getTownBlockCost()) + Colors.Gray + 
					(Double.valueOf(TownySettings.getClaimPriceIncreaseValue()).equals(1.0) ? "" : " | " + Colors.Green + "Increase per TownBlock: " + Colors.LightGreen + "+" +  new DecimalFormat("##.##%").format(TownySettings.getClaimPriceIncreaseValue()-1)));
			output.add(Colors.Yellow + "[Claiming] " + Colors.Green + "Outposts: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost()));
		}
		if (town == null)
			output.add(Colors.Yellow + "[Upkeep] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeep()) + Colors.Gray + " | " + Colors.Green + "Nation: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationUpkeep()));
		output.add(Colors.Gray + "Town upkeep is based on " + Colors.LightGreen + " the " + (TownySettings.isUpkeepByPlot() ? " number of plots" : " town level (num residents)."));
		if (TownySettings.getUpkeepPenalty() > 0 )
			output.add(Colors.Gray + "Overclaimed upkeep is based on " + Colors.LightGreen + (TownySettings.isUpkeepPenaltyByPlot() ? "the number of plots overclaimed * " + TownySettings.getUpkeepPenalty() : "a flat cost of " + TownySettings.getUpkeepPenalty()));

		if (town != null) {
			output.add(Colors.Yellow + "Town [" + town.getFormattedName() + "]");
			output.add(Colors.Rose + "    [Price] " + Colors.Green + "Plot: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(town.getPlotPrice()) + Colors.Gray + " | " + Colors.Green + "Outpost: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost()));
			output.add(Colors.Rose + "             " + Colors.Green + "Shop: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(town.getCommercialPlotPrice()) + Colors.Gray + " | " + Colors.Green + "Embassy: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(town.getEmbassyPlotPrice()));

			output.add(Colors.Rose + "    [Taxes] " + Colors.Green + "Resident: " + Colors.LightGreen + (town.isTaxPercentage()? town.getTaxes() + "%" : TownyEconomyHandler.getFormattedBalance(town.getTaxes())) + Colors.Gray + " | " + Colors.Green + "Plot: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(town.getPlotTax()));
			output.add(Colors.Rose + "              " + Colors.Green + "Shop: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(town.getCommercialPlotTax()) + Colors.Gray + " | " + Colors.Green + "Embassy: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(town.getEmbassyPlotTax()));

			output.add(Colors.Rose + "    [Setting Plots] " + Colors.Green + "Shop: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getPlotSetCommercialCost()) + Colors.Gray + " | " + Colors.Green + "Embassy: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getPlotSetEmbassyCost()) + Colors.Gray + " | "  + Colors.Green + "Wilds: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getPlotSetWildsCost()));
			output.add(Colors.Rose + "                      " + Colors.Green + "Inn: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getPlotSetInnCost()) + Colors.Gray + " | " + Colors.Green + "Jail: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getPlotSetJailCost()) + Colors.Gray + " | " + Colors.Green + "Farm: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getPlotSetFarmCost()));
			output.add(Colors.Rose + "                      " + Colors.Green + "Bank: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getPlotSetBankCost()));
			
			if (nation != null) {
				output.add(Colors.Yellow + "Nation [" + nation.getFormattedName() + "]");
				output.add(Colors.Rose + "    [Taxes] " + Colors.Green + "Town: " + Colors.LightGreen + nation.getTaxes() + Colors.Gray + " | " + Colors.Green + "Peace: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationNeutralityCost()));
			}
		}
		return output;
	}
	
	public List<String> getTopBankBalance(final List<Government> governments) {
		final int maxListing = TownySettings.getTownyTopSize();
		final List<String> output = new ArrayList<>();
		final Map<Government, Double> data = new HashMap<>();

		// Sort by their bank balance first
		governments.sort(GovernmentComparators.BY_BANK_BALANCE);
		// Reverse it to show top down
		Collections.reverse(governments);
		// Loop through each one (already sorted) and add to the map
		for (final Government gov : governments) {
			data.put(gov, gov.getAccount().getCachedBalance());
		}
		int index = 0;
		for (Map.Entry<Government, Double> entry : data.entrySet()) {
			index++;
			if (maxListing != -1 && index > maxListing) {
				break;
			}
			final Government government = entry.getKey();
			output.add(String.format(Colors.LightGray + "%-20s " + Colors.Gold + "|" + Colors.Blue + " %s", government.getFormattedName(), TownyEconomyHandler.getFormattedBalance(entry.getValue())));
		}
		return output;
	}

	public List<String> getMostResidents(List<ResidentList> list) {
		final int maxListing = TownySettings.getTownyTopSize();

		List<String> output = new ArrayList<>();
		KeyValueTable<ResidentList, Integer> kvTable = new KeyValueTable<>();
		for (ResidentList obj : list)
			kvTable.put(obj, obj.getResidents().size());
		kvTable.sortByValue();
		kvTable.reverse();
		int n = 0;
		for (KeyValue<ResidentList, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			ResidentList residentList = kv.key;
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", ((TownyObject) residentList).getFormattedName(), kv.value));
		}
		return output;
	}

	public List<String> getMostLand(List<TownBlockOwner> list) {
		final int maxListing = TownySettings.getTownyTopSize();

		List<String> output = new ArrayList<>();
		KeyValueTable<TownBlockOwner, Integer> kvTable = new KeyValueTable<>();
		for (TownBlockOwner obj : list)
			kvTable.put(obj, obj.getTownBlocks().size());
		kvTable.sortByValue();
		kvTable.reverse();
		int n = 0;
		for (KeyValue<TownBlockOwner, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			String name = null;
			if (kv.key instanceof Town)
				name = ((Town) kv.key).getFormattedName();
			else 
				name = ((Resident) kv.key).getFormattedName();
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", name, kv.value));
		}
		return output;
	}

	public void consoleUseOnly(Player player) {

		TownyMessaging.sendErrorMsg(player, "This command was designed for use in the console only.");
	}

	public void inGameUseOnly(CommandSender sender) {

		sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
	}

	public boolean sendErrorMsg(CommandSender sender, String msg) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			TownyMessaging.sendErrorMsg(player, msg);
		} else
			// Console
			sender.sendMessage("[Towny] ConsoleError: " + msg);

		return false;
	}
}
