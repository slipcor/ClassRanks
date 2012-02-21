package net.slipcor.classranks;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.slipcor.classranks.commands.ClassAdminCommand;
import net.slipcor.classranks.commands.ClassCommand;
import net.slipcor.classranks.commands.RankdownCommand;
import net.slipcor.classranks.commands.RankupCommand;
import net.slipcor.classranks.core.Rank;
import net.slipcor.classranks.listeners.*;
import net.slipcor.classranks.managers.ClassManager;
import net.slipcor.classranks.managers.CommandManager;
import net.slipcor.classranks.managers.DebugManager;
import net.slipcor.classranks.managers.FormatManager;
import net.slipcor.classranks.managers.PlayerManager;
import net.slipcor.classranks.permissions.*;
import net.slipcor.classranks.register.payment.Method;

/**
 * main plugin class
 * 
 * @version v0.2.2
 * 
 * @author slipcor
 */

public class ClassRanks extends JavaPlugin {
    private final CommandManager cmdMgr = new CommandManager(this);
    private final CRServerListener serverListener = new CRServerListener(this, cmdMgr);
	private final DebugManager db = new DebugManager(this);
	public boolean trackRanks = false;
    
    public Method method = null; // eConomy access
    public CRPermissionHandler perms; // Permissions access
    
    @Override
	public void onEnable() {

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(serverListener, this);
        
        @SuppressWarnings("unused")
		ClassManager cm = new ClassManager(this);

        getCommand("class").setExecutor(new ClassCommand(this,cmdMgr));
        getCommand("classadmin").setExecutor(new ClassAdminCommand(this,cmdMgr));
        getCommand("rankup").setExecutor(new RankupCommand(this,cmdMgr));
        getCommand("rankdown").setExecutor(new RankdownCommand(this,cmdMgr));
        
        load_config(); // load the config file
        
        if (pm.getPlugin("bPermissions") != null) {
        	db.i("bPermissions found!");
        	this.perms = new bPermissionsHandler(this);
        } else if (pm.getPlugin("PermissionsEx") != null) {
        	db.i("PermissionsEX found!");
        	this.perms = new PermissionsEXHandler(this);
        } else {
        	db.i("No perms found, defaulting to SuperPermissions!");
        	this.perms = new SuperPermissionsHandler(this);
        }
        
        this.perms.setupPermissions();
        
        Tracker tracker = new Tracker(this);
        tracker.start();
		Update.updateCheck(this);
        
        log("v" + this.getDescription().getVersion() + " enabled", Level.INFO);
    }
    
    @Override
	public void onDisable() {
    	Tracker.stop();
        log("disabled", Level.INFO);
    }
    
    /**
     * (re)load the config
     */
	public void load_config() {
		String debugVersion = "0225";
		
		if (getConfig() == null || !getConfig().getString("cversion").equals(debugVersion)) {
			log("creating default config.yml", Level.INFO);
			getConfig().set("cversion", debugVersion);
			getConfig().options().copyDefaults(true);
			saveConfig();
		}
        DebugManager.active = getConfig().getBoolean("debug",false);
        
        if (getConfig().getBoolean("checkprices") && getConfig().getConfigurationSection("prices") != null) {
        	db.i("prices are already set, reading...");
	        // set prices
	        Map<String, Object> prices = (Map<String, Object>) getConfig().getConfigurationSection("prices").getValues(true);
	        cmdMgr.moneyCost = new double[prices.size()];
	        int i = 0;
	        for (String Key : prices.keySet()) {
	        	String sVal = (String) prices.get(Key);
	        	try {
	        		cmdMgr.moneyCost[i] = Double.parseDouble(sVal);
	        		db.i("#" + i + " => " + String.valueOf(Double.parseDouble(sVal)));
	        	} catch (Exception e) {
	        		cmdMgr.moneyCost[i] = 0;
	        		log("Unrecognized cost key '" + String.valueOf(Key) + "': "+sVal, Level.INFO);
	        	}
	        	i++;
	        }
        }

        if (getConfig().getBoolean("checkexp") && getConfig().getConfigurationSection("exp") != null) {
	        // set exp prices

    		db.i("exp costs are set, reading...");
	        Map<String, Object> expprices = (Map<String, Object>) getConfig().getConfigurationSection("exp").getValues(true);
	        cmdMgr.expCost = new int[expprices.size()];
	        int i = 0;
	        for (String Key : expprices.keySet()) {
	        	String sVal = (String) expprices.get(Key);
	        	try {
	        		cmdMgr.expCost[i] = Integer.parseInt(sVal);
	        		db.i("#" + i + " => " + String.valueOf(Integer.parseInt(sVal)));
	        	} catch (Exception e) {
	        		cmdMgr.expCost[i] = 0;
	        		log("Unrecognized exp cost key '" + String.valueOf(Key) + "': "+sVal, Level.INFO);
	        	}
	        	i++;
	        }
        }
        
		// set subcolors
        cmdMgr.getFormatManager().setColors("world", getConfig().getString("playercolor"), getConfig().getString("worldcolor"));

		// set other variables
		cmdMgr.rankpublic = getConfig().getBoolean("rankpublic", false);
		cmdMgr.defaultrankallworlds = getConfig().getBoolean("defaultrankallworlds", false);
		cmdMgr.onlyoneclass = getConfig().getBoolean("onlyoneclass", true);
		trackRanks = getConfig().getBoolean("trackRanks", false);
		
		boolean signs = getConfig().getBoolean("signcheck", false);
		if (signs) {
    		db.i("sign check activated!");
			cmdMgr.signCheck[0] = getConfig().getString("signchoose","[choose]");
			cmdMgr.signCheck[1] = getConfig().getString("signrankup","[rankup]");
			cmdMgr.signCheck[2] = getConfig().getString("signrankdown","[rankdown]");
		}
		
		PlayerManager.coolDown = getConfig().getInt("cooldown", 0);
		
		ItemStack[][] itemStacks = null;
		if (getConfig().getBoolean("checkitems") && getConfig().getConfigurationSection("items") != null) {
    		db.i("items exist, parsing...");
			Map<String, Object> items = (Map<String, Object>) getConfig().getConfigurationSection("items").getValues(false);
			if (items == null) {
	    		db.i("items invalid, setting to null");
				itemStacks = new ItemStack[ClassManager.getClasses().size()][1];
			} else {
				// for each items => ItemStack[][1,2,3]
				int iI = 0;
				itemStacks = new ItemStack[items.size()][];
				for (String isKey : items.keySet()) {
					List<?> values = getConfig().getList("items." + isKey);
					itemStacks[iI] = new ItemStack[values.size()];
		    		db.i("creating itemstack:");
					for (int iJ = 0 ; iJ < values.size() ; iJ++) {
						String[] vValue = (String.valueOf(values.get(iJ))).split(":");
						
						int vAmount = vValue.length > 1 ? Integer.parseInt(vValue[1]) : 1;
						try {
							itemStacks[iI][iJ] = new ItemStack(
				                    Material.valueOf(vValue[0]),
				                    vAmount
				                );

						} catch (Exception e) {
							try {
								itemStacks[iI][iJ] = new ItemStack(
					                    Integer.valueOf(vValue[0]),
					                    vAmount
					                );
							} catch (Exception e2) {

								log("Unrecognized reagent: " + vValue[0], Level.WARNING);
								continue;
							}
						}
					}
					db.i(iI + " - " + FormatManager.formatItemStacks(itemStacks[iI]));
					iI++;
				}
			}
		}
		cmdMgr.rankItems = itemStacks;
		
		Map<String, Object> classRanks = getConfig().getConfigurationSection("classes").getValues(false);
		for (String sClassName : classRanks.keySet()) {
			Object cSec = classRanks.get(sClassName);
			ConfigurationSection cs = (ConfigurationSection) cSec;
			Map<String, Object> rank = cs.getValues(true);
			boolean newClass = true;
			for (String sDispName : rank.keySet()) {
				String value = (String) rank.get(sDispName);
				String[] s = ClassManager.validateDispNameColor(value);
				if (s == null) {
					log("Unrecognized content: " + value, Level.WARNING);
					continue;
				}
				
				if (newClass) {
					// create class
					ClassManager.configClassAdd(sClassName, sDispName, value, null);
					
					newClass = false;
				} else {
					ClassManager.configRankAdd(sClassName, sDispName, value, null);
				}
			}
		}
		
		saveConfig();
	}
	
	/**
	 * log a prefixed message to the logfile
	 * @param message the message to log
	 * @param level the logging level
	 */
	public void log(String message, Level level){
        Bukkit.getLogger().log(level,"[ClassRanks] " + message);
    }
	
	/**
	 * send a message to a player
	 * @param pPlayer the player to message
	 * @param string the string to send
	 */
	public void msg(Player pPlayer, String string) {
		pPlayer.sendMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] " + string);
		db.i("to " + pPlayer.getName() + ": "+ string);
	}

	/**
	 * send a message to a commandsender
	 * @param sender the commandsender to message
	 * @param string the string to send
	 */
	public void msg(CommandSender sender, String string) {
		sender.sendMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] " + string);
	}
	
	/**
	 * save the classrank map to the config
	 */
	public void save_config() {
		db.i("saving config...");
		for (net.slipcor.classranks.core.Class cClass : ClassManager.getClasses()) {
    		db.i(" - "+cClass.name);
			for (Rank rRank : cClass.ranks) {
				db.i("   - " + rRank.getPermName() + ": '" + rRank.getDispName() + ":&" + Integer.toHexString(rRank.getColor().ordinal()));

				getConfig().set("classes." + cClass.name + "." + rRank.getPermName(), String.valueOf(rRank.getDispName() + ":&" + Integer.toHexString(rRank.getColor().ordinal())));
			}
		}
		saveConfig();
	}
}
