package praxis.slipcor.classranksBP;

import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.bananaco.permissions.interfaces.PermissionSet;

import praxis.classranks.register.payment.Method.MethodAccount;
import praxis.slipcor.classranksBP.ClassRanks;
import praxis.slipcor.classranksBP.CRPlayers;

/*
 * classes access class
 * 
 * v0.1.4.4 - minor fixes
 * 
 * History:
 * 
 *      v0.1.4.3 - Multiworld "all" support
 *      v0.1.4.2 - Reagents => Items ; Cooldown ; Sign usage
 * 		v0.1.4.1 - method NPE Fix
 * 		v0.1.4.0 - Register API
 * 		v0.1.3.5 - Fix: rank world setting
 * 		v0.1.3.4 - Fix: cost parsing
 * 				 - Fix: class change announcement
 *          	 - Cleanup: static plugin access
 *          	 - Cleanup: ordering
 * 		v0.1.3.3 - Possibility to require items for upranking
 * 		v0.1.3.2 - little fix of auto completion, cleanup
 * 		v0.1.3.1 - database filling via content.yml
 * 		v0.1.3.0 - big rewrite: +SQL, +classadmin
 * 
 * @author slipcor
 */

public class CRClasses {
	public static ClassRanks plugin = null;
	
	public static Collection<Map<String, Object>> groups = new ArrayList<Map<String,Object>>(); // Map of groups players can choose
	public static ChatColor colPlayer = ChatColor.YELLOW; // Color: Playername
	public static ChatColor colWorld = ChatColor.GOLD;   // Color: Worldname
	public static double[] cost = new double[3]; // Costs
	public static boolean rankpublic; // do we spam changed to the public?
	public static boolean defaultrankallworlds; // do ppl rank themselves in all worlds?
	public static ItemStack[][] rankItems;
	public static int coolDown = 0; // CoolDown timer variable ( Seconds )
	public static String[] signCheck = {null, null, null}; // SignCheck variable

	/*
	 * This function searches the group map specified in the config.
	 * It checks the given string (case insensitive) and returns the
	 * exact Class:Options pair for further investigation
	 */
	public static String getEverythingbyIDs(int cID, int rID) {
		return mysqlReturnEverything("SELECT * FROM classranks_ranks WHERE cid = '" + String.valueOf(cID) + "' AND oid = '" + String.valueOf(rID) + "';");
	}
	
	
	/*
	 * This function searches the group map specified in the config.
	 * It checks the given string (case insensitive) and returns the
	 * exact Class:Options pair for further investigation
	 */
	public static String getEverythingbyClassName(String className) {
		return mysqlReturnEverything("SELECT * FROM classranks_ranks WHERE permname = '" + className + "';");
	}
	
	/*
	 * This function searches the permission groups.
	 * It checks player's permission in world and returns the
	 * exact Key:Class pair for further permission handling
	 */
	public static String getEverythingbyPlayer(String world, String player) {
		return mysqlReturnGroupByPlayer("SELECT * FROM classranks_ranks WHERE 1;",world,player);
	}

	/*
	 * This function uses getClassKeySetbyPlayer and returns the
	 * exact group for further permission handling
	 */
	public static String getClass(String world, String player) {
		// get the exact string alike "MTrader:Trader III:&2:1:2:2"
		String classExists = getEverythingbyPlayer(world, player);
		if (classExists.equals("")) {
			return ""; // No Rank, result empty
		}
		String[] spl = classExists.split(":");
		return spl[0]; // return the Group
	}
	
	/*
	 * This function provides the main ranking process - including error messages
	 * when stuff is not right.
	 *   - check if permissions are there
	 *   - get old class
	 *   - calculate new class
	 *   - delete from old group
	 *   - add to new group
	 */
	public static boolean rank(String[] args, Player comP) {
		int cDown = CRPlayers.coolDownCheck(comP);
		if (cDown > 0) {
			ClassRanks.pmsg(comP, "You have to wait " + ChatColor.RED + String.valueOf(cDown) + ChatColor.WHITE + " seconds!");
			return true;
		}
		
		if ((!hasPerms(comP, "classranks.rankdown", comP.getWorld().getName())) && args[0].equalsIgnoreCase("rankdown")) {
			// if we either are no OP or try to rank down, but we may not
			ClassRanks.pmsg(comP, "You don't have permission to rank down!");
			return true;
		}

		String World = defaultrankallworlds?"all":comP.getWorld().getName(); // store the player's world
		String changePlayer = comP.getName(); // store the player we want to change
		Boolean self = true; // we want to change ourselves ... maybe
		
		if (args.length > 1) { // we have more than one argument. that means:
			changePlayer = CRPlayers.search(args[1]); // we want to change a different player
			self = false; // we do not want to change ourselves
			if (args.length>2) { // we even have more than two arguments. that means:
				World = args[2]; // we want to specify a world where we change
			}
		}

		// do we have a class?
		String classExists = getEverythingbyPlayer(World, changePlayer);
		
		if (classExists.equals("")) {
			// No Class
			if (self) {
				ClassRanks.pmsg(comP, "You don't have a class!");
				return true;
			} else {
				ClassRanks.pmsg(comP, "Player " + CRFormats.applyColor(changePlayer,colPlayer) + " does not have a class !");
				return true;
			}
		}
		// extract old class from alike "MTrader:Trader III:&2:1:2:2"
		String[] tStr = classExists.split(":");
		
		String cPermName = tStr[0]; // Permissions rank name
		String cDispName = tStr[1]; // Display rank name
		String cColor = tStr[2];    // Rank color
		int cID = Integer.parseInt(tStr[3]); // Classes ID
		int cRank = Integer.parseInt(tStr[4]); // Classes tree rank
		int cMaxRank = Integer.parseInt(tStr[5]); // Classes max tree rank
		
		// placeholder: cost
		double rank_cost = 0;
		// placeholder: items
		ItemStack[] items = null;
		
		// placeholders for new class
		int rankOffset = 0;
		
		if (args[0].equalsIgnoreCase("rankup")) {
			// we want to rank up!
			if (cRank >= cMaxRank) {
				// if the player is in the maximum rank
				if (self) {
					ClassRanks.pmsg(comP, "You are already at highest rank!");
				} else {
					ClassRanks.pmsg(comP, "Player " + CRFormats.applyColor(changePlayer,colPlayer) + " already at highest rank!");
				}
				return true;
			}
			if (self) {
				// if we rank ourselves, that might cost money
				// (otherwise, thats not the players problem ;) )
				rank_cost = cost[cRank+1];
				
				if (ClassRanks.method != null) {
					MethodAccount ma = ClassRanks.method.getAccount(comP.getName());
					if (ma == null) {
						ClassRanks.log("Account not found: "+comP.getName(), Level.SEVERE);
						return true;
					}
					if(!ma.hasEnough(rank_cost)){
						// no money, no rank!
		                ClassRanks.pmsg(comP, "You don't have enough money to rank up!");
		                return true;
		            }
				}
				
				items = rankItems[cRank+1];
			}
			rankOffset = 1;
		} else if (args[0].equalsIgnoreCase("rankdown")) {
			if (cRank < 1) {
				// We are at lowest rank
				if (self) {
					ClassRanks.pmsg(comP, "You are already at lowest rank!");
				} else  {
					ClassRanks.pmsg(comP, "Player " + CRFormats.applyColor(changePlayer,colPlayer) + " already at lowest rank!");
				}
				return true;
			}
			rankOffset = -1;
		}

		if ((items != null) && (!CRFormats.formatItemStacks(items).equals(""))) {
			if (!CRPlayers.ifHasTakeItems(comP, items)) {
				ClassRanks.pmsg(comP, "You don't have the required items!");
				ClassRanks.pmsg(comP, "(" + CRFormats.formatItemStacks(items) + ")");
				return true;
			}
		}
		
		changePlayer = CRPlayers.search(changePlayer);
		classRemove(World, changePlayer, cPermName);
		
		// "MTrader:Trader III:&2:1:2:2"
		String[] nStr = getEverythingbyIDs(cID,cRank+rankOffset).split(":");
		
		cPermName = nStr[0];
		cDispName = nStr[1];
		cColor = nStr[2];

		classAdd(World, changePlayer, cPermName);
		
		if ((ClassRanks.method != null) && (rank_cost > 0)) {
			// take the money, inform the player

			MethodAccount ma = ClassRanks.method.getAccount(comP.getName());
			ma.subtract(rank_cost);
			comP.sendMessage(ChatColor.DARK_GREEN + "[" + ChatColor.WHITE + "Money" + ChatColor.DARK_GREEN +  "] " + ChatColor.RED + "Your account had " + ChatColor.WHITE + ClassRanks.method.format(rank_cost) + ChatColor.RED + " debited.");
		}
		
		if (self && !rankpublic) {
			// we rank ourselves and do NOT want that to be public
			ClassRanks.pmsg(comP, "You are now a " + CRFormats.formatStringByColorCode(cDispName, cColor) + " in " + CRFormats.applyColor(World,colWorld) + "!");
		} else {
			plugin.getServer().broadcastMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] Player " + CRFormats.applyColor(changePlayer, colPlayer) + " now is a " + CRFormats.formatStringByColorCode(cDispName, cColor) + " in " + CRFormats.applyColor(World,colWorld) + "!");
		}
		return true;
	}	
	
    public static boolean hasPerms(Player comP, String string, String world) {
    	return ClassRanks.permissionHandler.getPermissionSet(world).getPlayerNodes(comP).contains("string");
	}

    
	/*
     * Add a user to a given class in the given world
     */
	public static void classAdd(String world, String player, String cString) {
		player = CRPlayers.search(player); // auto-complete playername
		if (ClassRanks.permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = plugin.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					ClassRanks.permissionHandler.getPermissionSet(worlds[i]).addGroup(player, cString);
				} catch (Exception e) {
					ClassRanks.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}
	
	/*
	 * Remove a user from the class he has in the given world
	 */
	public static void classRemove(String world, String player, String cString) {
		player = CRPlayers.search(player); // auto-complete playername
		if (ClassRanks.permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = plugin.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					ClassRanks.permissionHandler.getPermissionSet(worlds[i]).removeGroup(player, cString);
				} catch (Exception e) {
					ClassRanks.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	/*
	 * The main switch for the command usage. Check for known commands,
	 * permissions, arguments, and commit whatever is wanted.
	 */
	public static boolean parseCommand(Player pPlayer, String[] args) {

		if (args.length > 1) {
			
			if (args[0].equalsIgnoreCase("get")) {
				String world = defaultrankallworlds?"all":pPlayer.getWorld().getName();
				if (args.length>2) {
					world = args[2];
				}
				args[1] = CRPlayers.search(args[1]);
				String className = getEverythingbyPlayer(world, args[1]);
				if (className.equals("")) {
					ClassRanks.pmsg(pPlayer,"Player " + CRFormats.applyColor(args[1],colPlayer) + " has no group in " + CRFormats.applyColor(world,colWorld) + "!");
				} else {
					// "MTrader:Trader III:&2:1:2"
					String[] tStr = className.split(":");
					String cDispName = tStr[1]; // Display rank name
					String cColor = tStr[2];    // Rank color
					
					ClassRanks.pmsg(pPlayer,"Player " + CRFormats.applyColor(args[1],colPlayer) + " is in group " + CRFormats.formatStringByColorCode(cDispName,cColor) + " in " + CRFormats.applyColor(world,colWorld) + "!");
				}
				return true;
			}
			
			// more than /class <classname> or /class rankup
			if (!hasPerms(pPlayer, "classranks.admin.rank", pPlayer.getWorld().getName())) {
				ClassRanks.pmsg(pPlayer,"You don't have permission to change other user's ranks!");
				return true;
			}
			
			if ((!args[0].equalsIgnoreCase("add")) && (!args[0].equalsIgnoreCase("remove"))) {
				if ((args[0].equalsIgnoreCase("rankup")) || (args[0].equalsIgnoreCase("rankdown"))) {
					// rank up or down
					if (args.length>1) {
						return rank( args,pPlayer);
					} else {
						ClassRanks.pmsg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
						return false;
					}
				}
				ClassRanks.pmsg(pPlayer,"Argument " + args[0] + " unrecognized!");
				return false;
			}
			// add / remove
			boolean self = true;
			String world = defaultrankallworlds?"all":pPlayer.getWorld().getName();
			if (args[0].equalsIgnoreCase("remove")) {
				if (args.length > 2) {
					// => /class remove *name* world
					world = args[2];
					self = false;
				} else if (args.length > 1) {
					self = false;
				}
			} else if (args[0].equalsIgnoreCase("add")) {
				if (args.length > 3) {
					// => /class add *name* *classname* world
					world = args[3];
				} else if (args.length == 3) {
					// => /class add *name* *classname*
					self = false;
				} else if (args.length == 2) {
					// => /class add *classname*
					
					String[] tArgs = {args[0],pPlayer.getName(),args[1]};
					
					args = tArgs; // override : pretend to have said: //class add me class
				} else if (args.length < 2) {
					// not enough arguments ;)
    				ClassRanks.pmsg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
    				return false;
    			}
			}
			
			if (!self) {
				if (!hasPerms(pPlayer, "classranks.admin.addremove", pPlayer.getWorld().getName())) {
					// only if we are NOT op or may NOT add/remove classes
					ClassRanks.pmsg(pPlayer,"You don't have permission to add/remove other player's classes!");
					return true;
				}
			}
			
			// get the class of the player we're talking about
			args[1] = CRPlayers.search(args[1]);
			String className = getClass(world, args[1]);

			if (!className.equals("")) {
				// player has a class
				className = getEverythingbyClassName(className);

				// "MTrader*Trader III:&2"
    			String[] tStr = className.split(":");
    			
				String cDispName = tStr[1]; // Display rank name
				String cColor = tStr[2];    // Rank color
    			
				if (args[0].equalsIgnoreCase("add")) {
					// only one class per player :p
					ClassRanks.pmsg(pPlayer,"Player " + CRFormats.applyColor(args[1],colPlayer) + " already is in the class " + CRFormats.formatStringByColorCode(cDispName,cColor) + "!");
				} else if (args[0].equalsIgnoreCase("remove")) {
					classRemove(world, args[1], tStr[0]); // do it!
					
					if ((!pPlayer.getName().equalsIgnoreCase(args[1]))) {
						ClassRanks.pmsg(pPlayer,"Player " + CRFormats.applyColor(args[1],colPlayer) + " removed from Group " + CRFormats.formatStringByColorCode(cDispName,cColor) + " in " + CRFormats.applyColor(world,colWorld) + "!");

						Player chP = plugin.getServer().getPlayer(args[1]);
						try {
							if (chP.isOnline()) {
								chP.sendMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] You were removed from Group " + CRFormats.formatStringByColorCode(cDispName,cColor) + " in " + CRFormats.applyColor(world,colWorld) + "!");
							}
						} catch (Exception e) {
							// do nothing, the player is not online
						}
					} else {
						// self remove successful!
						ClassRanks.pmsg(pPlayer,"You were removed from Group " + CRFormats.formatStringByColorCode(cDispName,cColor) + " in " + CRFormats.applyColor(world,colWorld) + "!");
					}
				}
				return true;
			}
			// player has no class
			if (!args[0].equalsIgnoreCase("add")) {
				// no class and trying to remove
				if (pPlayer.getName().equalsIgnoreCase(args[1])) {
    				ClassRanks.pmsg(pPlayer,"You don't have a class!");
    				return true;
					
				}else {
    				ClassRanks.pmsg(pPlayer,"Player " + CRFormats.applyColor(args[1], colPlayer) + " does not have a class!");
    				return true;
				}
			}
			// ADD

			className = getEverythingbyClassName(args[2]);
			
			if (className.equals("")) {
				ClassRanks.pmsg(pPlayer,"The class you have entered does not exist!");
				return true;
			}
			
			// "MTrader*Trader III:&2"
			String[] tStr = className.split(":");
			String cPermName = tStr[0]; // Display rank name
			String cDispName = tStr[1]; // Display rank name
			String cColor = tStr[2];    // Rank color
			
			classAdd(world, args[1], cPermName);
			if ((rankpublic) || (!pPlayer.getName().equalsIgnoreCase(args[1]))) {
				plugin.getServer().broadcastMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] " + CRFormats.applyColor(args[1],colPlayer) + " now is a " + CRFormats.formatStringByColorCode(cDispName,cColor));
				return true;
			} else {
				ClassRanks.pmsg(pPlayer,"You now are a " + CRFormats.formatStringByColorCode(cDispName,cColor));
				return true;
			}
		} else if (args.length > 0) {
			// only ONE argument!
			
			if (args[0].equalsIgnoreCase("reload")) {
				if (pPlayer.isOp()) {
					plugin.loadConfig();
					ClassRanks.pmsg(pPlayer,"Config reloaded!");
				} else {
					ClassRanks.pmsg(pPlayer,"You don't have permission to reload!");
				}
				return true;
			}
			
			if (args[0].equalsIgnoreCase("dbload")) {
				if (pPlayer.isOp()) {
					plugin.loadDatabase(pPlayer);
					ClassRanks.pmsg(pPlayer,"Database loaded!");
				} else {
					ClassRanks.pmsg(pPlayer,"You don't have permission to access the database!");
				}
				return true;
			}
			
			if (args[0].equalsIgnoreCase("rankup")) {
    			if (!hasPerms(pPlayer, "classranks.self.rank", pPlayer.getWorld().getName())) {
    				ClassRanks.pmsg(pPlayer,"You don't have permission to rank yourself up!");
    				return true;
    			}
				return rank(args, pPlayer);
			} else if (args[0].equalsIgnoreCase("rankdown")) {
    			if (!hasPerms(pPlayer, "classranks.self.rank", pPlayer.getWorld().getName())) {
    				ClassRanks.pmsg(pPlayer,"You don't have permission to rank yourself down!");
    				return true;
    			}
				return rank(args, pPlayer);
			} else if (args[0].equalsIgnoreCase("remove")) {
    			if (!hasPerms(pPlayer, "classranks.self.addremove", pPlayer.getWorld().getName())) {
    				ClassRanks.pmsg(pPlayer,"You don't have permission to add/remove your rank!");
    				return true;
    			}
    			String className = getClass(pPlayer.getWorld().getName(), pPlayer.getName());
				
    			className = getEverythingbyClassName(className);
				
				if (className.equals("")) {
					ClassRanks.pmsg(pPlayer,"You don't have a class!");
					return true;
				}

				// "MTrader*Trader III:&2"
    			String[] tStr = className.split(":");
    			
    			String cDispName = tStr[1]; // Display rank name
    			String cColor = tStr[2];    // Rank color
    			
    			classRemove(defaultrankallworlds?"all":pPlayer.getWorld().getName(), pPlayer.getName(), tStr[0]);

    			ClassRanks.pmsg(pPlayer,"You were removed from Group " + CRFormats.formatStringByColorCode(cDispName,cColor) + " in " + CRFormats.applyColor(defaultrankallworlds?"all":pPlayer.getWorld().getName(),colWorld) + "!");
				return true;
			} else if (args[0].equalsIgnoreCase("add")) {
				ClassRanks.pmsg(pPlayer,"Not enough arguments!");
				return true;
    			
			}
			
			// /class [classname]
			
    		// Check if the player has got the money
            if (ClassRanks.method != null) {
				MethodAccount ma = ClassRanks.method.getAccount(pPlayer.getName());
            	if (!ma.hasEnough(cost[0])) {
            
	                ClassRanks.pmsg(pPlayer,"You don't have enough money to choose your class!");
	                return true;
            	}
            }

			String className = getClass(pPlayer.getWorld().getName(), pPlayer.getName());
			
			if (!className.equals("")) {
				ClassRanks.pmsg(pPlayer,"You already are in the Class " + className + "!");
				return true;
			}
		
			className = getEverythingbyClassName(args[0]);
			
			if (className.equals("")) {
				ClassRanks.pmsg(pPlayer,"The class you have entered does not exist!");
				return true;
			}
			
			if ((rankItems[0] != null) && (!CRFormats.formatItemStacks(rankItems[0]).equals(""))) {
				if (!CRPlayers.ifHasTakeItems(pPlayer, rankItems[0])) {
					ClassRanks.pmsg(pPlayer, "You don't have the required items!");
					ClassRanks.pmsg(pPlayer, "(" + CRFormats.formatItemStacks(rankItems[0]) + ")");
					return true;
				}
			}
			
			// "MTrader*Trader III:&2"
			String[] tStr = className.split(":");

			String cPermName = tStr[0]; // Display rank name
			String cDispName = tStr[1]; // Display rank name
			String cColor = tStr[2];    // Rank color
			int cRank = Integer.parseInt(tStr[4]); // Classes tree rank

			if (cRank > 0) {
				ClassRanks.pmsg(pPlayer,"The class you entered is no starting class!");
				return true;
			}
			
			// success!

			classAdd(defaultrankallworlds?"all":pPlayer.getWorld().getName(), pPlayer.getName(), cPermName);
			if (rankpublic) {
				plugin.getServer().broadcastMessage("Player " + CRFormats.applyColor(pPlayer.getName(),colPlayer) +" has chosen the class " + CRFormats.formatStringByColorCode(cDispName,cColor));
			} else {
				ClassRanks.pmsg(pPlayer,"You have chosen your class! You now are a " + CRFormats.formatStringByColorCode(cDispName,cColor));
			}	
			
			if ((ClassRanks.method != null) && (cost[0] > 0)) {
				// if it costs anything at all

				MethodAccount ma = ClassRanks.method.getAccount(pPlayer.getName());
				ma.subtract(cost[0]);
				pPlayer.sendMessage(ChatColor.DARK_GREEN + "[" + ChatColor.WHITE + "Money" + ChatColor.DARK_GREEN +  "] " + ChatColor.RED + "Your account had " + ChatColor.WHITE + ClassRanks.method.format(cost[0]) + ChatColor.RED + " debited.");
			}
			return true;
		}
		ClassRanks.pmsg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
		return false;
	}


	public static boolean parseAdminCommand(Player pPlayer, String[] args) {
		if (!hasPerms(pPlayer,"classranks.admin.admin",pPlayer.getWorld().getName())) {
			ClassRanks.pmsg(pPlayer,"You don't have permission to administrate ranks!");
			return true;
		}
		
		if (args.length<3) {
			ClassRanks.pmsg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
			return false;
		}
		
		if (args[0].equalsIgnoreCase("remove")) {
			/*
			 *  /classadmin remove class *classname*
			 *  /classadmin remove rank *classname* *rankname*
			 */
			if (args[1].equalsIgnoreCase("class")) {
				if (args.length!=3) {
					ClassRanks.pmsg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
					return false;
				}
				return configClassRemove(args[2], pPlayer);
			} else if (args[1].equalsIgnoreCase("rank")) {
				if (args.length!=4) {
					ClassRanks.pmsg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
					return false;
				}
				return configRankRemove(args[2],args[3], pPlayer);
			}
			// second argument unknown
			return false;
		} else if (args[0].equalsIgnoreCase("add")) {
			
			/*
			 *  /classadmin add class *classname* *rankname* *displayname* *color*
			 *  /classadmin add rank *classname* *rankname* *displayname* *color*
			 */
			
			if (args.length!=6) {
				ClassRanks.pmsg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
				return false;
			}
			if (args[1].equalsIgnoreCase("class")) {
				return configClassAdd(args[2],args[3],args[4] + ":" + args[5], pPlayer);
			} else if (args[1].equalsIgnoreCase("rank")) {
				return configRankAdd(args[2],args[3],args[4] + ":" + args[5], pPlayer);
			}
			// second argument unknown
			return false;
		} else if (args[0].equalsIgnoreCase("change")) {
			
			/*
			 *  /classadmin change rank *classname* *rankname* *displayname* *color*
			 *  /classadmin change class *classname* *newclassname*
			 */
			
			if (args[1].equalsIgnoreCase("class")) {
				if (args.length!=4) {
					ClassRanks.pmsg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
					return false;
				}
				return configClassChange(args[2],args[3], pPlayer);
			} else if (args[1].equalsIgnoreCase("rank")) {
				if (args.length!=6) {
					ClassRanks.pmsg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
					return false;
				}
				return configRankChange(args[2],args[3],args[4] + ":" + args[5], pPlayer);
			}
			// second argument unknown
			return false;
		}
		return false;
	}

	public static void mysqlQuery(String query) {
		if (plugin.MySQL) {
			try {
				if (query.startsWith("UPDATE")) {
					plugin.manageMySQL.updateQuery(query);
				} else if (query.startsWith("DELETE")) {
					plugin.manageMySQL.deleteQuery(query);
				} else if (query.startsWith("INSERT")) {
					plugin.manageMySQL.insertQuery(query);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			if (query.startsWith("UPDATE")) {
				plugin.manageSQLite.updateQuery(query);
			} else if (query.startsWith("DELETE")) {
				plugin.manageSQLite.deleteQuery(query);
			} else if (query.startsWith("INSERT")) {
				plugin.manageSQLite.insertQuery(query);
			}			
		}
	}

	public static String mysqlReturnEverything(String query) {
		String resultString = "";
		ResultSet result = null;
		if (plugin.MySQL) {
			try {
				result = plugin.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = plugin.manageSQLite.sqlQuery(query);
		}
		try {

			// "MTrader:Trader III:&2:1:2:2"
			if (result != null && result.next()) {
				String permName = result.getString("permname");
				String dispName = result.getString("dispname");
				String color = "&" + String.valueOf(Integer.toHexString(result.getInt("color")));
				int cID = result.getInt("cid");
				int rID = result.getInt("oid");
				int rMax = mysqlReturnMaxRank("SELECT * FROM classranks_ranks WHERE cid = '" + cID + "';");
				
				resultString = permName + ":" + dispName + ":" + color + ":";
				resultString += String.valueOf(cID) + ":" + String.valueOf(rID) + ":" +String.valueOf(rMax);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return resultString;
	}

	public static int mysqlReturnMaxRank(String query) {
		ResultSet result = null;
		if (plugin.MySQL) {
			try {
				result = plugin.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = plugin.manageSQLite.sqlQuery(query);
		}
		int count = -1;
		try {
			while (result != null && result.next()) {
				int rID = result.getInt("oid");
				if (rID > count) {
					count = rID;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count;
	}
	
	public static String mysqlReturnGroupByPlayer(String query, String world, String player) {
		ResultSet result = null;
		if (plugin.MySQL) {
			try {
				result = plugin.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = plugin.manageSQLite.sqlQuery(query);
		}
		try {
			while (result != null && result.next()) {				
				String permName = result.getString("permname");
				
				String[] worlds = {world};
				if (world.equalsIgnoreCase("all")) {
					List<World> lWorlds = plugin.getServer().getWorlds();
					
					worlds = new String[lWorlds.size()];
					for(int i=0;i<lWorlds.size();i++) {
						worlds[i] = lWorlds.get(i).getName();
					}
				}
				for(int i=0;i<worlds.length;i++) {
					if (isInGroup(worlds[i], permName, player)) { // If the current key is the class name player belongs to
						
						String dispName = result.getString("dispname");
						String color = "&" + String.valueOf(Integer.toHexString(result.getInt("color")));
						int cID = result.getInt("cid");
						int rID = result.getInt("oid");
						int rMax = mysqlReturnMaxRank("SELECT * FROM classranks_ranks WHERE cid = '" + cID + "';");
						
						String resultString = permName + ":" + dispName + ":" + color + ":";
						resultString += String.valueOf(cID) + ":" + String.valueOf(rID) + ":" +String.valueOf(rMax);
						return resultString;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static int mysqlReturnLastClassID(String query) {
		ResultSet result = null;
		if (plugin.MySQL) {
			try {
				result = plugin.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = plugin.manageSQLite.sqlQuery(query);
		}
		int count = 0;
		try {
			while (result != null && result.next()) {
				int cID = result.getInt("id");
				if (cID > count) {
					count = cID;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count;
	}

	public static int mysqlReturnCIDByClassName(String rClassName) {
		ResultSet result = null;
		String query = "SELECT * FROM classranks_classes WHERE 1;";
		if (plugin.MySQL) {
			try {
				result = plugin.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = plugin.manageSQLite.sqlQuery(query);
		}
		try {
			while (result != null && result.next()) {
				String classname = result.getString("classname");
				if (classname.equalsIgnoreCase(rClassName)) {
					return result.getInt("id");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static boolean isInGroup(String world, String permName, String player) {
		PermissionSet ps = ClassRanks.permissionHandler.getPermissionSet(world);
		List<String> list = ps.getGroups(player);
		
		if (list.contains(permName)) {
			return true;
		}

		return false;
	}
	
	public static boolean configRankChange(String rClassName, String rPermName, String rValue, Player pPlayer) {
		String[] tStr = rValue.replace("_", " ").split(":");
		tStr[1] = String.valueOf(Integer.parseInt(tStr[1].substring(1),16));
		mysqlQuery("UPDATE classranks_ranks SET dispname = '" + tStr[0] + "', color = '" + tStr[1] + "' WHERE permname = '" + rPermName + "';");
		ClassRanks.pmsg(pPlayer,"Class " + rClassName + " => Rank " + rPermName + " set to '" + tStr[0] + ":" + tStr[1]);
		return true;
	}

	public static boolean configClassChange(String cClassName, String cNewName, Player pPlayer) {
		mysqlQuery("UPDATE classranks_classes SET classname = '" + cNewName + "' WHERE classname = '" + cClassName + "';");
		ClassRanks.pmsg(pPlayer,"Class " + cClassName + " renamed to " + cNewName);
		return true;
	}

	public static boolean configRankAdd(String rClassName, String rPermName, String rValue, Player pPlayer) {
		int cID = mysqlReturnCIDByClassName(rClassName);
		if (cID > -1) {
			String[] tStr = rValue.replace("_", " ").split(":");
			tStr[1] = String.valueOf(Integer.parseInt(tStr[1].substring(1),16));
			
			int rMax = mysqlReturnMaxRank("SELECT * FROM classranks_ranks WHERE cid = '" + cID + "';")+1;
			
			mysqlQuery("INSERT INTO classranks_ranks (cid, oid, permname, dispname, color) VALUES(" + String.valueOf(cID) + ", " + String.valueOf(rMax) + ", '" + rPermName + "', '" + tStr[0] + "', '" + tStr[1] + "');");
			ClassRanks.pmsg(pPlayer,"Class " + rClassName + " => Rank #" + String.valueOf(rMax) + " " + rPermName + " added: '" + tStr[0] + ":" + tStr[1]);
			return true;
		}
		ClassRanks.pmsg(pPlayer,"Class " + rClassName + " not found!");
		return true;
	}

	public static boolean configClassAdd(String cClassName, String cPermName, String cValue, Player pPlayer) {
		if (plugin.MySQL) {
			mysqlQuery("INSERT INTO classranks_classes (classname) VALUES ('" + cClassName + "');");
		} else {
			int newID = mysqlReturnLastClassID("SELECT * FROM classranks_classes WHERE 1;");
			mysqlQuery("INSERT INTO classranks_classes (id, classname) VALUES (" + String.valueOf(++newID) + ", '" + cClassName + "');");
		}
		ClassRanks.pmsg(pPlayer,"Class " + cClassName + " added");
		configRankAdd(cClassName, cPermName, cValue, pPlayer);
		
		return true;
	}

	public static boolean configRankRemove(String rClassName, String rPermName, Player pPlayer) {
		mysqlQuery("DELETE FROM classranks_ranks WHERE permname = '" + rPermName + "';");
		ClassRanks.pmsg(pPlayer,"Class " + rClassName + " => Rank " + rPermName + " deleted");
		return true;
	}

	public static boolean configClassRemove(String cClassName, Player pPlayer) {
		int cID = mysqlReturnCIDByClassName(cClassName);
		if (cID > -1) {
			mysqlQuery("DELETE FROM classranks_classes WHERE id = '" + cID + "';");
			mysqlQuery("DELETE FROM classranks_ranks WHERE cid = '" + cID + "';");
			ClassRanks.pmsg(pPlayer,"Class " + cClassName + " deleted");
			return true;
		}
		ClassRanks.pmsg(pPlayer,"Class " + cClassName + " not found!");
		return true;
	}
}
