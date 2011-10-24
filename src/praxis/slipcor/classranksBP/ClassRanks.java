package praxis.slipcor.classranksBP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.alta189.sqlLibrary.MySQL.mysqlCore;
import com.alta189.sqlLibrary.SQLite.sqlCore;
import de.bananaco.permissions.Permissions;
import de.bananaco.permissions.worlds.WorldPermissionsManager;
import praxis.classranks.register.payment.Method;
import praxis.slipcor.classranksBP.CRClasses;
import praxis.slipcor.classranksBP.CRFormats;

/*
 * main class
 * 
 * v0.1.4.5 - update to CB #1337
 * 
 * History:
 * 
 *      v0.1.4.4 - minor fixes
 *      v0.1.4.3 - Multiworld "all" support
 *      v0.1.4.2 - Reagents => Items ; Cooldown ; Sign usage
 * 		v0.1.4.0 - Register API
 *      v0.1.3.4 - Fix: cost parsing
 * 			     - Fix: class change announcement
 *               - Cleanup: static plugin access
 *               - Cleanup: ordering
 * 		v0.1.3.3 - Possibility to require items for upranking
 * 		v0.1.3.2 - little fix of auto completion, cleanup
 * 		v0.1.3.1 - database filling via content.yml
 * 		v0.1.3.0 - big rewrite: +SQL, +classadmin
 * 		v0.1.2.8 - rewritten config, ready for ingame ranks and permissionsbukkit
 * 		v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 		v0.1.2.6 - corrected permission nodes
 * 		v0.1.2.3 - world and player color customizable
 * 		v0.1.2.0 - renaming for release
 * 
 * 2do:
 * 		
 * @author slipcor
 */

public class ClassRanks extends JavaPlugin {
    private final CRPlayerListener playerListener = new CRPlayerListener();
    public static CRServerListener serverListener = new CRServerListener();
	public static WorldPermissionsManager permissionHandler; // Permissions access
	public static Method method = null; // eConomy access
    private static Logger Logger; // Logfile access
	//mySQL access
	public mysqlCore manageMySQL; // MySQL handler
	public sqlCore manageSQLite; // SQLite handler

	// Settings Variables
	public Boolean MySQL = false;
	public String dbHost = null;
	public String dbUser = null;
	public String dbPass = null;
	public String dbDatabase = null;
    
	/*
	 * Function that gets executed when players use a command
	 * (non-Javadoc)
	 * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
	 */	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if ((cmd.getName().equalsIgnoreCase("rankup")) || (cmd.getName().equalsIgnoreCase("rankdown"))) {
			// if we use the shortcut /rankup or /rankdown, shift the array
			String[] tStr = new String[args.length+1];
			System.arraycopy(args, 0, tStr, 1, args.length);
			tStr[0] = cmd.getName();
			return CRClasses.parseCommand((Player) sender, tStr);
    	}

    	if (cmd.getName().equalsIgnoreCase("class")){
    		// standard class command, parse it!
    		return CRClasses.parseCommand((Player) sender, args);
    	}
    	if (cmd.getName().equalsIgnoreCase("classadmin")) {
    		// admin class command, parse it!
    		return CRClasses.parseAdminCommand((Player) sender, args);
    	}
		return true;
    }
    
	/*
	 * Function that gets executed on plugin activation
	 * (non-Javadoc)
	 * @see org.bukkit.plugin.Plugin#onEnable()
	 */
	public void onEnable(){    	
        Logger = java.util.logging.Logger.getLogger("Minecraft");

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Normal, this);
        
        loadConfig(); // load the config file       

        if (!setupPermissions()) {
        	// Disable plugin, because useless without Permissions
        	getServer().getPluginManager().disablePlugin(this);
            return;
        }
        CRClasses.plugin = this; // hand over plugin
        PluginDescriptionFile pdfFile = this.getDescription();
        
        log("v" + pdfFile.getVersion() + " enabled", Level.INFO);
    }

	/*
	 * Function that stores the values out of the config.yml into the plugin
	 */
	public void loadConfig() {
    	if(!this.getDataFolder().exists()){
            this.getDataFolder().mkdir();
        }

        File fConfig = new File(this.getDataFolder(),"config.yml");
        if(!fConfig.isFile()){
            try{ // save the default config.yml (from jar) into the data folder
                File jarloc = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile();
                if(jarloc.isFile()){
                    JarFile jar = new JarFile(jarloc);
                    JarEntry entry = jar.getJarEntry("config.yml");
                    
                    if(entry != null && !entry.isDirectory()){
                        InputStream in = jar.getInputStream(entry);
                        FileOutputStream out = new FileOutputStream(fConfig);
                        byte[] tempbytes = new byte[512];
                        int readbytes = in.read(tempbytes,0,512);
                        while(readbytes>-1){
                            out.write(tempbytes,0,readbytes);
                            readbytes = in.read(tempbytes,0,512);
                        }
                        out.close();
                        in.close();

                        log("Created default config.yml", Level.INFO);
                    }
                }
            }catch(Exception e){
                log("Unable to create default config.yml:" + e, Level.INFO);
            }
        }
        
        YamlConfiguration config = new YamlConfiguration();
        try {
			config.load(fConfig);
		} catch (FileNotFoundException e1) {
			log("File not found!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			log("IO Exception!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (InvalidConfigurationException e1) {
			log("Invalid Configuration!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (Exception e) {
			log("Did you update to v0.1.5? - Backup and remove your config!", Level.SEVERE);
			e.printStackTrace();
			return;
		}
        
        // set prices
        Map<String, Object> prices = (Map<String, Object>) config.getConfigurationSection("prices").getValues(true);
        CRClasses.cost = new double[prices.size()];
        int i = 0;
        for (String Key : prices.keySet()) {
        	String sVal = (String) prices.get(Key);
        	try {
        		CRClasses.cost[i] = Double.parseDouble(sVal);
        	} catch (Exception e) {
        		CRClasses.cost[i] = 0;
        		log("Unrecognized cost key '" + String.valueOf(Key) + "': "+sVal, Level.INFO);
        	}
        	i++;
        }
        
		// set subcolors
		CRClasses.colPlayer = CRFormats.cColorbyCode(config.getString("playercolor"));
		CRClasses.colWorld = CRFormats.cColorbyCode(config.getString("worldcolor"));

		// set other variables
		CRClasses.rankpublic = config.getBoolean("rankpublic", false);
		CRClasses.defaultrankallworlds = config.getBoolean("defaultrankallworlds", false);
		CRClasses.onlyoneclass = config.getBoolean("onlyoneclass", true);
		
		boolean signs = config.getBoolean("signcheck", false);
		if (signs) {
			CRClasses.signCheck[0] = config.getString("signchoose","[choose]");
			CRClasses.signCheck[1] = config.getString("signrankup","[rankup]");
			CRClasses.signCheck[2] = config.getString("signrankdown","[rankdown]");
		}
		
		CRClasses.coolDown = config.getInt("cooldown", 0);
		
		ItemStack[][] itemStacks = null;
		if (config.getConfigurationSection("items") != null) {
			Map<String, Object> items = (Map<String, Object>) config.getConfigurationSection("items").getValues(true);
			if (items == null) {
				itemStacks = new ItemStack[3][1];
			} else {
				// for each items => ItemStack[][1,2,3]
				int iI = 0;
				itemStacks = new ItemStack[items.size()][];
				for (String isKey : items.keySet()) {
					String values = (String) items.get(isKey);
					String[] vStr = values.split(" ");
					itemStacks[iI] = new ItemStack[vStr.length];
					for (int iJ = 0 ; iJ < vStr.length ; iJ++) {
	
						String[] vValue = vStr[iJ].split(":");
						
						int vAmount = vValue.length > 1 ? Integer.parseInt(vValue[1]) : 1;
						try {
							itemStacks[iI][iJ] = new ItemStack(
				                    Material.valueOf(vValue[0]),
				                    vAmount
				                );
						} catch (Exception e) {
							log("Unrecognized reagent: " + vValue[0], Level.WARNING);
							continue;
						}
					}
					iI++;
				}
			}

		}
		CRClasses.rankItems = itemStacks;
		// get variables from settings handler
 		if (config.getBoolean("MySQL", false)) {
 			this.MySQL = config.getBoolean("MySQL", false);
 			this.dbHost = config.getString("MySQLhost");
 			this.dbUser = config.getString("MySQLuser");
 			this.dbPass = config.getString("MySQLpass");
 			this.dbDatabase = config.getString("MySQLdb");
 		}
 		
 		// Check Settings
 		if (this.MySQL) {
 			if (this.dbHost.equals(null)) { this.MySQL = false; log("MySQL is on, but host is not defined, defaulting to SQLite", Level.SEVERE); }
 			if (this.dbUser.equals(null)) { this.MySQL = false; log("MySQL is on, but username is not defined, defaulting to SQLite", Level.SEVERE); }
 			if (this.dbPass.equals(null)) { this.MySQL = false; log("MySQL is on, but password is not defined, defaulting to SQLite", Level.SEVERE); }
 			if (this.dbDatabase.equals(null)) { this.MySQL = false; log("MySQL is on, but database is not defined, defaulting to SQLite", Level.SEVERE); }
 		}
 		
 		// Enabled SQL/MySQL
 		if (this.MySQL) {
 			// Declare MySQL Handler
 			this.manageMySQL = new mysqlCore(Logger, "[ClassRanks] ", this.dbHost, this.dbDatabase, this.dbUser, this.dbPass);
 			
 			log("MySQL Initializing", Level.INFO);
 			// Initialize MySQL Handler
 			this.manageMySQL.initialize();
 			
 			try {
 				if (this.manageMySQL.checkConnection()) {
 					log("MySQL connection successful", Level.INFO);
 	 				// Check if the tables exist, if not, create them
 					if (!this.manageMySQL.checkTable("classranks_classes")) {
 						log("Creating table classranks_classes", Level.INFO);
 						String query = "CREATE TABLE `classranks_classes` ( `id` int(3) NOT NULL AUTO_INCREMENT, `classname` varchar(42) NOT NULL, PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
 						this.manageMySQL.createTable(query);
 					}
 					if (!this.manageMySQL.checkTable("classranks_ranks")) { // Check if the table exists in the database if not create it
 						log("Creating table classranks_ranks", Level.INFO);
 						String query = "CREATE TABLE `classranks_ranks` ( `id` int(4) NOT NULL AUTO_INCREMENT, `cid` int(3) NOT NULL, `oid` int(20) NOT NULL, `permname` varchar(42) NOT NULL, `dispname` varchar(42) DEFAULT NULL, `color` int(2) NOT NULL DEFAULT '15', PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
 						this.manageMySQL.createTable(query);
 					}
 				} else {
 					log("MySQL connection failed", Level.SEVERE);
 					this.MySQL = false;
 				}
 			} catch (MalformedURLException e) {
 				e.printStackTrace();
 			} catch (InstantiationException e) {
 				e.printStackTrace();
 			} catch (IllegalAccessException e) {
 				e.printStackTrace();
 			}
 		} else {
 			log("SQLite Initializing", Level.INFO);
 			
 			// Declare SQLite handler
 			this.manageSQLite = new sqlCore(Logger, "[ClassRanks]", "ClassRanks", this.getDataFolder().toString());
 			
 			// Initialize SQLite handler
 			this.manageSQLite.initialize();

			// Check if the tables exist, if not, create them
 			if (!this.manageSQLite.checkTable("classranks_classes")) {
 				log("Creating classranks_classes", Level.INFO);
 				String query = "CREATE TABLE `classranks_classes` ( `id` int(3) PRIMARY KEY, `classname` varchar(42) NOT NULL );";
				this.manageSQLite.createTable(query); // Use sqlCore.createTable(query) to create tables 
 			}
 			if (!this.manageSQLite.checkTable("classranks_ranks")) {
 				log("Creating classranks_ranks", Level.INFO);
 				String query = "CREATE TABLE `classranks_ranks` ( `id` int(4) PRIMARY KEY, `cid` int(3) NOT NULL, `oid` int(20) NOT NULL, `permname` varchar(42) NOT NULL, `dispname` varchar(42) DEFAULT NULL, `color` int(2) NOT NULL DEFAULT '15' );";
				this.manageSQLite.createTable(query); // Use sqlCore.createTable(query) to create tables 
 			}
 		}
	}
    
    /*
     * Function that gets executed on plugin deactivation
     * (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
	public void onDisable(){
        log("disabled", Level.INFO);
    }
    
	/*
	 * Function that logs a message to the logfile
	 */
    public static void log(String message, Level level){
        Logger.log(level,"[ClassRanks] " + message);
    }

    /*
     * Function that tries to setup the permissions system, returns result
     */
    private boolean setupPermissions() {
    	// try to load permissions, return result
    	
        ClassRanks.permissionHandler = Permissions.getWorldPermissionsManager();
        if(ClassRanks.permissionHandler == null){
        	log("bPermissions not found, deactivating.", Level.SEVERE);
            return false;            
        }
        log("<3 bPermissions", Level.INFO);
    	return true;
    }

    /*
     * Function that adds a prefix to a string and sends that to given player
     */
	public static void pmsg(Player pPlayer, String string) {
		pPlayer.sendMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] " + string);
	}

	/*
	 * Function that loads the database with values given in content.yml
	 */
	@SuppressWarnings("unchecked")
	public void loadDatabase(Player pPlayer) {
        File fConfig = new File(this.getDataFolder(),"content.yml");
        if(!fConfig.isFile()){
            try{ // save the default config.yml (from jar) into the data folder
                File jarloc = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile();
                if(jarloc.isFile()){
                    JarFile jar = new JarFile(jarloc);
                    JarEntry entry = jar.getJarEntry("content.yml");
                    
                    if(entry != null && !entry.isDirectory()){
                        InputStream in = jar.getInputStream(entry);
                        FileOutputStream out = new FileOutputStream(fConfig);
                        byte[] tempbytes = new byte[512];
                        int readbytes = in.read(tempbytes,0,512);
                        while(readbytes>-1){
                            out.write(tempbytes,0,readbytes);
                            readbytes = in.read(tempbytes,0,512);
                        }
                        out.close();
                        in.close();

                        log("Created default content.yml", Level.INFO);
                    }
                }
            }catch(Exception e){
                log("Unable to create default content.yml:" + e, Level.INFO);
            }
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
			config.load(fConfig);
		} catch (FileNotFoundException e1) {
			log("File not found!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			log("IO Exception!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (InvalidConfigurationException e1) {
			log("Invalid Configuration!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (Exception e) {
			log("Did you update to v0.1.5? - Backup and remove your config!", Level.SEVERE);
			e.printStackTrace();
			return;
		}

        Map<String, Object> contents = (Map<String, Object>) config.getConfigurationSection("classes").getValues(true);
        for (String cClass : contents.keySet()) {
        	log(cClass, Level.INFO);
        	boolean first = true;
        	Map<String, String> ranks = (Map<String,String>) contents.get(cClass);
        	for (String rRank : ranks.keySet()) {
        		log(rRank, Level.INFO);
        		if (first) {
        			// class add
        			CRClasses.configClassAdd(cClass, rRank, ranks.get(rRank), pPlayer);
        			first = false;
        		} else {
        			// rank add
        			CRClasses.configRankAdd(cClass, rRank, ranks.get(rRank), pPlayer);
        		}
        	}
        }
	}
}
