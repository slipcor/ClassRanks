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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.alta189.sqlLibrary.MySQL.mysqlCore;
import com.alta189.sqlLibrary.SQLite.sqlCore;
import praxis.classranks.register.payment.Method;
import praxis.slipcor.classranksBP.CRClasses;

/*
 * main class
 * 
 * v0.1.6 - cleanup
 * 
 * History:
 * 
 *     v0.1.5.2 - dbload correction, onlyoneclass activation
 *     v0.1.5.1 - cleanup
 *     v0.1.5.0 - more fixes, update to CB #1337
 *     v0.1.4.4 - minor fixes
 *     v0.1.4.3 - Multiworld "all" support
 *     v0.1.4.2 - Reagents => Items ; Cooldown ; Sign usage
 * 	   v0.1.4.0 - Register API
 *     v0.1.3.4 - Fix: cost parsing
 * 			     - Fix: class change announcement
 *               - Cleanup: static plugin access
 *               - Cleanup: ordering
 * 	   v0.1.3.3 - Possibility to require items for upranking
 * 	   v0.1.3.2 - little fix of auto completion, cleanup
 * 	   v0.1.3.1 - database filling via content.yml
 * 	   v0.1.3.0 - big rewrite: +SQL, +classadmin
 * 	   v0.1.2.8 - rewritten config, ready for ingame ranks and permissionsbukkit
 * 	   v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 	   v0.1.2.6 - corrected permission nodes
 * 	   v0.1.2.3 - world and player color customizable
 * 	   v0.1.2.0 - renaming for release
 * 		
 * 2do:
 *     read/improve
 * 
 * @author slipcor
 */

public class ClassRanks extends JavaPlugin {
    private final CRClasses classes = new CRClasses(this);
    private final CRPlayerListener playerListener = new CRPlayerListener(classes);
    private final CRServerListener serverListener = new CRServerListener(this);
    private Logger Logger; // Logfile access
	
    Method method = null; // eConomy access
    mysqlCore manageMySQL; // MySQL handler
    sqlCore manageSQLite; // SQLite handler

	// Settings Variables
	Boolean MySQL = false;
	private String dbHost = null;
	private String dbUser = null;
	private String dbPass = null;
	private String dbDatabase = null;
    
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
			return classes.parseCommand((Player) sender, tStr);
    	}

    	if (cmd.getName().equalsIgnoreCase("class")){
    		// standard class command, parse it!
    		return classes.parseCommand((Player) sender, args);
    	}
    	if (cmd.getName().equalsIgnoreCase("classadmin")) {
    		// admin class command, parse it!
    		return classes.parseAdminCommand((Player) sender, args);
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
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Normal, this);
        
        loadConfig(); // load the config file       

        if (!classes.setupPermissions()) {
        	// Disable plugin, because useless without Permissions
        	getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        log("v" + this.getDescription().getVersion() + " enabled", Level.INFO);
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
	 * Function that stores the values out of the config.yml into the plugin
	 */
	void loadConfig() {
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
		} catch (FileNotFoundException e) {
			log("File not found!", Level.SEVERE);
			e.printStackTrace();
			return;
		} catch (IOException e) {
			log("IO Exception!", Level.SEVERE);
			e.printStackTrace();
			return;
		} catch (InvalidConfigurationException e) {
			log("Invalid Configuration!", Level.SEVERE);
			e.printStackTrace();
			return;
		} catch (Exception e) {
			log("Did you update to v0.1.5? - Backup and remove your config!", Level.SEVERE);
			e.printStackTrace();
			return;
		}
        
        if (config.getConfigurationSection("prices") != null) {
	        // set prices
	        Map<String, Object> prices = (Map<String, Object>) config.getConfigurationSection("prices").getValues(true);
	        classes.cost = new double[prices.size()];
	        int i = 0;
	        for (String Key : prices.keySet()) {
	        	String sVal = (String) prices.get(Key);
	        	try {
	        		classes.cost[i] = Double.parseDouble(sVal);
	        	} catch (Exception e) {
	        		classes.cost[i] = 0;
	        		log("Unrecognized cost key '" + String.valueOf(Key) + "': "+sVal, Level.INFO);
	        	}
	        	i++;
	        }
        }
        
		// set subcolors
        classes.f.setColors("world", config.getString("playercolor"), config.getString("worldcolor"));

		// set other variables
		classes.rankpublic = config.getBoolean("rankpublic", false);
		classes.defaultrankallworlds = config.getBoolean("defaultrankallworlds", false);
		classes.onlyoneclass = config.getBoolean("onlyoneclass", true);
		
		boolean signs = config.getBoolean("signcheck", false);
		if (signs) {
			classes.signCheck[0] = config.getString("signchoose","[choose]");
			classes.signCheck[1] = config.getString("signrankup","[rankup]");
			classes.signCheck[2] = config.getString("signrankdown","[rankdown]");
		}
		
		classes.crp.coolDown = config.getInt("cooldown", 0);
		
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
		classes.rankItems = itemStacks;
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
 			if (this.dbHost == null) { this.MySQL = false; log("MySQL is on, but host is not defined, defaulting to SQLite", Level.SEVERE); }
 			if (this.dbUser == null) { this.MySQL = false; log("MySQL is on, but username is not defined, defaulting to SQLite", Level.SEVERE); }
 			if (this.dbPass == null) { this.MySQL = false; log("MySQL is on, but password is not defined, defaulting to SQLite", Level.SEVERE); }
 			if (this.dbDatabase == null) { this.MySQL = false; log("MySQL is on, but database is not defined, defaulting to SQLite", Level.SEVERE); }
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
	 * Function that logs a message to the logfile
	 */
    void log(String message, Level level){
        Logger.log(level,"[ClassRanks] " + message);
    }

    /*
     * Function that adds a prefix to a string and sends that to given player
     */
	void msg(Player pPlayer, String string) {
		pPlayer.sendMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] " + string);
	}

    /*
     * Function that adds a prefix to a string and sends that to given sender
     */
	void msg(CommandSender sender, String string) {
		sender.sendMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] " + string);
	}
}
