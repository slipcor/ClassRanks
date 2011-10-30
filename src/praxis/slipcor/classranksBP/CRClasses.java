package praxis.slipcor.classranksBP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.bananaco.permissions.Permissions;
import de.bananaco.permissions.interfaces.PermissionSet;
import de.bananaco.permissions.worlds.WorldPermissionsManager;
import praxis.classranks.register.payment.Method.MethodAccount;
import praxis.slipcor.classranksBP.ClassRanks;
import praxis.slipcor.classranksBP.CRPlayers;

/*
 * classes access class
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
 * 	   v0.1.4.1 - method NPE Fix
 * 	   v0.1.4.0 - Register API
 * 	   v0.1.3.5 - Fix: rank world setting
 * 	   v0.1.3.4 - Fix: cost parsing
 * 	            - Fix: class change announcement
 *              - Cleanup: static plugin access
 *              - Cleanup: ordering
 * 	   v0.1.3.3 - Possibility to require items for upranking
 * 	   v0.1.3.2 - little fix of auto completion, cleanup
 * 	   v0.1.3.1 - database filling via content.yml
 * 	   v0.1.3.0 - big rewrite: +SQL, +classadmin
 * 
 * @author slipcor
 */

public class CRClasses {
	private ClassRanks c;
	private WorldPermissionsManager permissionHandler; // Permissions access
	CRFormats f;
	CRPlayers crp;
    
	double[] cost = new double[3]; // Costs
	boolean rankpublic; // do we spam changed to the public?
	boolean defaultrankallworlds; // do ppl rank themselves in all worlds?
	boolean onlyoneclass; // only maintain one class
	ItemStack[][] rankItems;
	int cool__Down = 0; // CoolDown timer variable ( Seconds )
	String[] signCheck = {null, null, null}; // SignCheck variable
	
	public CRClasses(ClassRanks plugin) {
		c = plugin;
		crp = new CRPlayers(plugin);
		f = new CRFormats(crp);
	}
	
	/*
	 * This function searches a given world and player
	 * Returns the permname
	 */
	public String getPermName(String world, String player) {
		// get the exact string alike "MTrader:Trader III:&2:1:2:2"
		String sRankInfo = getEverythingbyPlayer(world, player);
		if (sRankInfo.equals("")) {
			return ""; // No Rank, result empty
		}
		String[] spl = sRankInfo.split(":");
		return spl[0]; // return the permname
	}
	
	/*
	 * This function provides the main ranking process - including error messages
	 * when stuff is not right.
	 *   - check if permissions are there
	 *   - get old rank
	 *   - calculate new rank
	 *   - eventually delete from old rank
	 *   - add to new rank
	 */
	public boolean rank(String[] args, Player comP) {
		int cDown = crp.coolDownCheck(comP);
		if (cDown > 0) {
			c.msg(comP, "You have to wait " + ChatColor.RED + String.valueOf(cDown) + ChatColor.WHITE + " seconds!");
			return true; // cooldown timer still counting => OUT!
		}
		
		if ((!hasPerms(comP, "classranks.rankdown", comP.getWorld().getName())) && args[0].equalsIgnoreCase("rankdown")) {
			// if we either are no OP or try to rank down, but we may not
			c.msg(comP, "You don't have permission to rank down!");
			return true;
		}

		String World = defaultrankallworlds?"all":comP.getWorld().getName(); // store the player's world
		String changePlayer = comP.getName(); // store the player we want to change
		Boolean self = true; // we want to change ourselves ... maybe
		
		if (args.length > 1) { // we have more than one argument. that means:
			changePlayer = crp.search(args[1]); // we want to change a different player
			self = false; // we do not want to change ourselves
			if (args.length>2) { // we even have more than two arguments. that means:
				World = args[2]; // we want to specify a world where we change
			}
		}

		// do we have a rank?
		String hasRank = getEverythingbyPlayer(World, changePlayer);
		
		if (hasRank.equals("")) {
			// No Rank
			if (self) {
				c.msg(comP, "You don't have a class!");
				return true;
			} else {
				c.msg(comP, "Player " + f.formatPlayer(changePlayer) + " does not have a class !");
				return true;
			}
		}
		// extract old rank e.g. "MTrader:Trader III:&2:1:2:2"
		String[] tStr = hasRank.split(":");
		
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
		
		// placeholders for new rank
		int rankOffset = 0;
		
		if (args[0].equalsIgnoreCase("rankup")) {
			// we want to rank up!
			if (cRank >= cMaxRank) {
				// if the player is in the maximum rank
				if (self) {
					c.msg(comP, "You are already at highest rank!");
				} else {
					c.msg(comP, "Player " + f.formatPlayer(changePlayer) + " already at highest rank!");
				}
				return true;
			}
			if (self) {
				// if we rank ourselves, that might cost money
				// (otherwise, thats not the players problem ;) )
				rank_cost = cost[cRank+1];
				
				if (c.method != null) {
					MethodAccount ma = c.method.getAccount(comP.getName());
					if (ma == null) {
						c.log("Account not found: "+comP.getName(), Level.SEVERE);
						return true;
					}
					if (!ma.hasEnough(rank_cost)){
						// no money, no ranking!
		                c.msg(comP, "You don't have enough money to rank up!");
		                return true;
		            }
				}
				
				items = rankItems!=null?rankItems[cRank+1]:null;
			}
			rankOffset = 1;
		} else if (args[0].equalsIgnoreCase("rankdown")) {
			if (cRank < 1) {
				// We are at lowest rank
				if (self) {
					c.msg(comP, "You are already at lowest rank!");
				} else  {
					c.msg(comP, "Player " + f.formatPlayer(changePlayer) + " already at lowest rank!");
				}
				return true;
			}
			rankOffset = -1;
		}

		if ((items != null) && (!f.formatItemStacks(items).equals(""))) {
			if (!crp.ifHasTakeItems(comP, items)) {
				c.msg(comP, "You don't have the required items!");
				c.msg(comP, "(" + f.formatItemStacks(items) + ")");
				return true;
			}
		}
		
		changePlayer = crp.search(changePlayer);
		if (onlyoneclass || rankOffset < 0) // up and only OR down
			classRemove(World, changePlayer, cPermName);
		
		// "MTrader:Trader III:&2:1:2:2"
		String[] nStr = getEverythingbyIDs(cID,cRank+rankOffset).split(":");
		
		cPermName = nStr[0];
		cDispName = nStr[1];
		cColor = nStr[2];

		rankAdd(World, changePlayer, cPermName);
		
		if ((c.method != null) && (rank_cost > 0)) {
			// take the money, inform the player

			MethodAccount ma = c.method.getAccount(comP.getName());
			ma.subtract(rank_cost);
			comP.sendMessage(ChatColor.DARK_GREEN + "[" + ChatColor.WHITE + "Money" + ChatColor.DARK_GREEN +  "] " + ChatColor.RED + "Your account had " + ChatColor.WHITE + c.method.format(rank_cost) + ChatColor.RED + " debited.");
		}
		
		if (self && !rankpublic) {
			// we rank ourselves and do NOT want that to be public
			c.msg(comP, "You are now a " + f.formatStringByColorCode(cDispName, cColor) + " in " + f.formatWorld(World) + "!");
		} else {
			c.getServer().broadcastMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] Player " + f.formatPlayer(changePlayer) + " now is a " + f.formatStringByColorCode(cDispName, cColor) + " in " + f.formatWorld(World) + "!");
		}
		return true;
	}	

	public boolean isInGroup(String world, String permName, String player) {
		PermissionSet ps = permissionHandler.getPermissionSet(world);
		List<String> list = ps.getGroups(player);
		
		if (list.contains(permName)) {
			return true;
		}

		return false;
	}

	/*
	 * The main switch for the command usage. Check for known commands,
	 * permissions, arguments, and commit whatever is wanted.
	 */
	public boolean parseCommand(Player pPlayer, String[] args) {

		if (args.length > 1) {
			if (args[0].equalsIgnoreCase("get")) {
				String world = defaultrankallworlds?"all":pPlayer.getWorld().getName();
				if (args.length>2) {
					world = args[2];
				}
				args[1] = crp.search(args[1]);
				String className = getEverythingbyPlayer(world, args[1]);
				if (className.equals("")) {
					c.msg(pPlayer,"Player " + f.formatPlayer(args[1]) + " has no class in " + f.formatWorld(world) + "!");
				} else {
					// "MTrader:Trader III:&2:1:2"
					String[] tStr = className.split(":");
					String cDispName = tStr[1]; // Display rank name
					String cColor = tStr[2];    // Rank color
					
					c.msg(pPlayer,"Player " + f.formatPlayer(args[1]) + " is " + f.formatStringByColorCode(cDispName,cColor) + " in " + f.formatWorld(world) + "!");
				}
				return true;
			}
			
			// more than /class <classname> or /class rankup
			if (!hasPerms(pPlayer, "classranks.admin.rank", pPlayer.getWorld().getName())) {
				c.msg(pPlayer,"You don't have permission to change other user's ranks!");
				return true;
			}
			
			if ((!args[0].equalsIgnoreCase("add")) && (!args[0].equalsIgnoreCase("remove"))) {
				if ((args[0].equalsIgnoreCase("rankup")) || (args[0].equalsIgnoreCase("rankdown"))) {
					// rank up or down
					if (args.length>1) {
						return rank( args,pPlayer);
					} else {
						c.msg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
						return false;
					}
				}
				c.msg(pPlayer,"Argument " + args[0] + " unrecognized!");
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
    				c.msg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
    				return false;
    			}
			}
			
			if (!self) {
				if (!hasPerms(pPlayer, "classranks.admin.addremove", pPlayer.getWorld().getName())) {
					// only if we are NOT op or may NOT add/remove classes
					c.msg(pPlayer,"You don't have permission to add/remove other player's classes!");
					return true;
				}
			}
			
			// get the class of the player we're talking about
			args[1] = crp.search(args[1]);
			String sRankInfo = getPermName(world, args[1]);
			
			if (!sRankInfo.equals("")) {
				int i = 0; // combo breaker
				while (!sRankInfo.equals("")) {
					// player has a rank
					sRankInfo = getEverythingbyPermName(sRankInfo);
	
					// "MTrader*Trader III:&2"
	    			String[] tStr = sRankInfo.split(":");
	    			
					String cDispName = tStr[1]; // Display rank name
					String cColor = tStr[2];    // Rank color
	    			
					if (args[0].equalsIgnoreCase("add")) {
						// only one class per player :p
						c.msg(pPlayer,"Player " + f.formatPlayer(args[1]) + " already already has the rank " + f.formatStringByColorCode(cDispName,cColor) + "!");
					} else if (args[0].equalsIgnoreCase("remove")) {
						classRemove(world, args[1], tStr[0]); // do it!
						
						if ((!pPlayer.getName().equalsIgnoreCase(args[1]))) {
							c.msg(pPlayer,"Player " + f.formatPlayer(args[1]) + " removed from rank " + f.formatStringByColorCode(cDispName,cColor) + " in " + f.formatWorld(world) + "!");
	
							Player chP = c.getServer().getPlayer(args[1]);
							try {
								if (chP.isOnline()) {
									chP.sendMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] You were removed from rank " + f.formatStringByColorCode(cDispName,cColor) + " in " + f.formatWorld(world) + "!");
								}
							} catch (Exception e) {
								// do nothing, the player is not online
							}
						} else {
							// self remove successful!
							if (i == 0)
								c.msg(pPlayer,"You were removed from rank " + f.formatStringByColorCode(cDispName,cColor) + " in " + f.formatWorld(world) + "!");
						}
					}
					if (++i > 10) {
						c.log("Infinite loop! More than 10 ranks!?", Level.SEVERE);
						break;
					}
					sRankInfo = getPermName(world, args[1]);
				}
				return true;
			}
			// player has no class
			if (!args[0].equalsIgnoreCase("add")) {
				// no class and trying to remove
				if (pPlayer.getName().equalsIgnoreCase(args[1])) {
    				c.msg(pPlayer,"You don't have a class!");
    				return true;
				} else {
    				c.msg(pPlayer,"Player " + f.formatPlayer(args[1]) + " does not have a class!");
    				return true;
				}
			}
			// ADD

			sRankInfo = getEverythingbyPermName(args[2]);
			
			if (sRankInfo.equals("")) {
				c.msg(pPlayer,"The class you have entered does not exist!");
				return true;
			}
			
			// "MTrader*Trader III:&2"
			String[] tStr = sRankInfo.split(":");
			String cPermName = tStr[0]; // Display rank name
			String cDispName = tStr[1]; // Display rank name
			String cColor = tStr[2];    // Rank color
			
			classAdd(world, args[1], cPermName);
			if ((rankpublic) || (!pPlayer.getName().equalsIgnoreCase(args[1]))) {
				c.getServer().broadcastMessage("[" + ChatColor.AQUA + "ClassRanks" + ChatColor.WHITE + "] " + f.formatPlayer(args[1]) + " now is a " + f.formatStringByColorCode(cDispName,cColor));
				return true;
			} else {
				c.msg(pPlayer,"You now are a " + f.formatStringByColorCode(cDispName,cColor));
				return true;
			}
		} else if (args.length > 0) {
			// only ONE argument!
			
			if (args[0].equalsIgnoreCase("reload")) {
				if (pPlayer.isOp()) {
					c.loadConfig();
					c.msg(pPlayer,"Config reloaded!");
				} else {
					c.msg(pPlayer,"You don't have permission to reload!");
				}
				return true;
			}
			
			if (args[0].equalsIgnoreCase("dbload")) {
				if (pPlayer.isOp()) {
					loadDatabase(pPlayer);
					c.msg(pPlayer,"Database loaded!");
				} else {
					c.msg(pPlayer,"You don't have permission to access the database!");
				}
				return true;
			}
			
			if (args[0].equalsIgnoreCase("rankup")) {
    			if (!hasPerms(pPlayer, "classranks.self.rank", pPlayer.getWorld().getName())) {
    				c.msg(pPlayer,"You don't have permission to rank yourself up!");
    				return true;
    			}
				return rank(args, pPlayer);
			} else if (args[0].equalsIgnoreCase("rankdown")) {
    			if (!hasPerms(pPlayer, "classranks.self.rank", pPlayer.getWorld().getName())) {
    				c.msg(pPlayer,"You don't have permission to rank yourself down!");
    				return true;
    			}
				return rank(args, pPlayer);
			} else if (args[0].equalsIgnoreCase("remove")) {
    			if (!hasPerms(pPlayer, "classranks.self.addremove", pPlayer.getWorld().getName())) {
    				c.msg(pPlayer,"You don't have permission to add/remove your rank!");
    				return true;
    			}
    			String sRankInfo = getPermName(pPlayer.getWorld().getName(), pPlayer.getName());
				
    			sRankInfo = getEverythingbyPermName(sRankInfo);
				
				if (sRankInfo.equals("")) {
					c.msg(pPlayer,"You don't have a class!");
					return true;
				}

				// "MTrader*Trader III:&2"
    			String[] tStr = sRankInfo.split(":");
    			
    			String cDispName = tStr[1]; // Display rank name
    			String cColor = tStr[2];    // Rank color
    			
    			classRemove(defaultrankallworlds?"all":pPlayer.getWorld().getName(), pPlayer.getName(), tStr[0]);

    			c.msg(pPlayer,"You were removed from rank " + f.formatStringByColorCode(cDispName,cColor) + " in " + f.formatWorld(defaultrankallworlds?"all":pPlayer.getWorld().getName()) + "!");
				return true;
			} else if (args[0].equalsIgnoreCase("add")) {
				c.msg(pPlayer,"Not enough arguments!");
				return true;
    			
			}
			
			// /class [classname]
			
    		// Check if the player has got the money
            if (c.method != null) {
				MethodAccount ma = c.method.getAccount(pPlayer.getName());
            	if (!ma.hasEnough(cost[0])) {
            
	                c.msg(pPlayer,"You don't have enough money to choose your class! (" + c.method.format(cost[0]) + ")");
	                return true;
            	}
            }

			String sRankInfo = getPermName(pPlayer.getWorld().getName(), pPlayer.getName());
			
			if (!sRankInfo.equals("")) {
				c.msg(pPlayer,"You already have the rank " + sRankInfo + "!");
				return true;
			}
		
			sRankInfo = getEverythingbyPermName(args[0]);
			
			if (sRankInfo.equals("")) {
				c.msg(pPlayer,"The class you have entered does not exist!");
				return true;
			}
			
			if (rankItems != null && (rankItems[0] != null) && (!f.formatItemStacks(rankItems[0]).equals(""))) {
				if (!crp.ifHasTakeItems(pPlayer, rankItems[0])) {
					c.msg(pPlayer, "You don't have the required items!");
					c.msg(pPlayer, "(" + f.formatItemStacks(rankItems[0]) + ")");
					return true;
				}
			}
			
			// "MTrader*Trader III:&2"
			String[] tStr = sRankInfo.split(":");

			String cPermName = tStr[0]; // Display rank name
			String cDispName = tStr[1]; // Display rank name
			String cColor = tStr[2];    // Rank color
			
			// success!

			classAdd(defaultrankallworlds?"all":pPlayer.getWorld().getName(), pPlayer.getName(), cPermName);
			if (rankpublic) {
				c.getServer().broadcastMessage("Player " + f.formatPlayer(pPlayer.getName()) +" has chosen a class, now has the rank " + f.formatStringByColorCode(cDispName,cColor));
			} else {
				c.msg(pPlayer,"You have chosen your class! You now have the rank " + f.formatStringByColorCode(cDispName,cColor));
			}	
			
			if ((c.method != null) && (cost[0] > 0)) {
				// if it costs anything at all

				MethodAccount ma = c.method.getAccount(pPlayer.getName());
				ma.subtract(cost[0]);
				pPlayer.sendMessage(ChatColor.DARK_GREEN + "[" + ChatColor.WHITE + "Money" + ChatColor.DARK_GREEN +  "] " + ChatColor.RED + "Your account had " + ChatColor.WHITE + c.method.format(cost[0]) + ChatColor.RED + " debited.");
			}
			return true;
		}
		c.msg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
		return false;
	}


	public boolean parseAdminCommand(Player pPlayer, String[] args) {
		if (!hasPerms(pPlayer,"classranks.admin.admin",pPlayer.getWorld().getName())) {
			c.msg(pPlayer,"You don't have permission to administrate ranks!");
			return true;
		}
		
		if (args.length<3) {
			c.msg(pPlayer,"Not enough arguments (" + String.valueOf(args.length) + ")!");
			return false;
		}
		
		if (args[0].equalsIgnoreCase("remove")) {
			/*
			 *  /classadmin remove class *classname*
			 *  /classadmin remove rank *classname* *rankname*
			 */
			if (args[1].equalsIgnoreCase("class")) {
				if (args.length!=3) {
					c.msg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
					return false;
				}
				return configClassRemove(args[2], pPlayer);
			} else if (args[1].equalsIgnoreCase("rank")) {
				if (args.length!=4) {
					c.msg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
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
				c.msg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
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
					c.msg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
					return false;
				}
				return configClassChange(args[2],args[3], pPlayer);
			} else if (args[1].equalsIgnoreCase("rank")) {
				if (args.length!=6) {
					c.msg(pPlayer,"Wrong number of arguments (" + String.valueOf(args.length) + ")!");
					return false;
				}
				return configRankChange(args[2],args[3],args[4] + ":" + args[5], pPlayer);
			}
			// second argument unknown
			return false;
		}
		return false;
	}


    /*
     * Function that tries to setup the permissions system, returns result
     */
    boolean setupPermissions() {
    	// try to load permissions, return result
    	
        permissionHandler = Permissions.getWorldPermissionsManager();
        if(permissionHandler == null){
        	c.log("bPermissions not found, defaulting to OP.", Level.WARNING);
            return false;            
        }
        c.log("<3 bPermissions", Level.INFO);
    	return true;
    }
	
	/*
	 * This function searches the ranks database for exact matches of class ID and rank ID.
	 * Returns the detailed rank string with all possible information
	 */
	private String getEverythingbyIDs(int cID, int rID) {
		return mysqlReturnEverything("SELECT * FROM classranks_ranks WHERE cid = '" + String.valueOf(cID) + "' AND oid = '" + String.valueOf(rID) + "';");
	}
	
	/*
	 * This function searches the ranks database for exact matches of permission name.
	 * Returns the detailed rank string with all possible information
	 */
	private String getEverythingbyPermName(String permName) {
		return mysqlReturnEverything("SELECT * FROM classranks_ranks WHERE permname = '" + permName + "';");
	}
	
	/*
	 * This function searches the ranks database for exact matches of world and player name.
	 * Returns the detailed rank string with all possible information
	 */
	private String getEverythingbyPlayer(String world, String player) {
		return mysqlReturnEverythingByPlayer("SELECT * FROM classranks_ranks WHERE 1 ORDER BY `id` DESC;",world,player);
	}

	/*
	 * This function reads the CID of the given class name
	 * Returns the permname with the highest ID
	 */
	private String getFirstPermNamebyClass(String cString) {
		int cID = mysqlReturnCIDByClassName(cString);
		return mysqlReturnSinglePermName("SELECT * FROM `classranks_ranks` WHERE `cid` = '" + cID + "' ORDER BY `id` ASC");
	}
	
    private boolean hasPerms(Player comP, String string, String world) {
    	return permissionHandler.getPermissionSet(world).getPlayerNodes(comP).contains(string);
	}

    
	/*
     * Add a user to a given class in the given world
     */
	private void classAdd(String world, String player, String cString) {
		player = crp.search(player); // auto-complete playername
		cString = getFirstPermNamebyClass(cString);
		if (permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = c.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					permissionHandler.getPermissionSet(worlds[i]).addGroup(player, cString);
				} catch (Exception e) {
					c.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

    /*
     * Add a user to a given rank in the given world
     */
	private void rankAdd(String world, String player, String rank) {
		player = crp.search(player); // auto-complete playername
		if (permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = c.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					permissionHandler.getPermissionSet(worlds[i]).addGroup(player, rank);
				} catch (Exception e) {
					c.log("PermName " + rank + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	/*
	 * Remove a user from the class he has in the given world
	 */
	private void classRemove(String world, String player, String cString) {
		player = crp.search(player); // auto-complete playername
		if (permissionHandler != null) {
			String[] worlds = {world};
			if (world.equalsIgnoreCase("all")) {
				List<World> lWorlds = c.getServer().getWorlds();
				
				worlds = new String[lWorlds.size()];
				for(int i=0;i<lWorlds.size();i++) {
					worlds[i] = lWorlds.get(i).getName();
				}
			}
			for(int i=0;i<worlds.length;i++) {
				try {
					permissionHandler.getPermissionSet(worlds[i]).removeGroup(player, cString);
				} catch (Exception e) {
					c.log("PermName " + cString + " or user " + player + " not found in world " + worlds[i], Level.WARNING);
				}
			}
		}
	}

	private void mysqlQuery(String query) {
		if (c.MySQL) {
			try {
				if (query.startsWith("UPDATE")) {
					c.manageMySQL.updateQuery(query);
				} else if (query.startsWith("DELETE")) {
					c.manageMySQL.deleteQuery(query);
				} else if (query.startsWith("INSERT")) {
					c.manageMySQL.insertQuery(query);
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
				c.manageSQLite.updateQuery(query);
			} else if (query.startsWith("DELETE")) {
				c.manageSQLite.deleteQuery(query);
			} else if (query.startsWith("INSERT")) {
				c.manageSQLite.insertQuery(query);
			}			
		}
	}

	private String mysqlReturnEverything(String query) {
		String resultString = "";
		ResultSet result = null;
		if (c.MySQL) {
			try {
				result = c.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = c.manageSQLite.sqlQuery(query);
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

	private String mysqlReturnSinglePermName(String query) {
		String resultString = "";
		ResultSet result = null;
		if (c.MySQL) {
			try {
				result = c.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = c.manageSQLite.sqlQuery(query);
		}
		try {
			// "MTrader:Trader III:&2:1:2:2"
			if (result != null && result.next()) {
				return result.getString("permname");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return resultString;
	}
	
	private int mysqlReturnMaxRank(String query) {
		ResultSet result = null;
		if (c.MySQL) {
			try {
				result = c.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = c.manageSQLite.sqlQuery(query);
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
	
	private String mysqlReturnEverythingByPlayer(String query, String world, String player) {
		ResultSet result = null;
		if (c.MySQL) {
			try {
				result = c.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = c.manageSQLite.sqlQuery(query);
		}
		try {
			while (result != null && result.next()) {				
				String permName = result.getString("permname");
				
				String[] worlds = {world};
				if (world.equalsIgnoreCase("all")) {
					List<World> lWorlds = c.getServer().getWorlds();
					
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

	private int mysqlReturnHighestID(String query) {
		ResultSet result = null;
		if (c.MySQL) {
			try {
				result = c.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = c.manageSQLite.sqlQuery(query);
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

	private int mysqlReturnCIDByClassName(String rClassName) {
		ResultSet result = null;
		String query = "SELECT * FROM classranks_classes WHERE 1;";
		if (c.MySQL) {
			try {
				result = c.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = c.manageSQLite.sqlQuery(query);
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
	
	private boolean configRankChange(String rClassName, String rPermName, String rValue, Player pPlayer) {
		String[] tStr = rValue.replace("_", " ").split(":");
		tStr[1] = String.valueOf(Integer.parseInt(tStr[1].substring(1),16));
		mysqlQuery("UPDATE classranks_ranks SET dispname = '" + tStr[0] + "', color = '" + tStr[1] + "' WHERE permname = '" + rPermName + "';");
		c.msg(pPlayer,"Class " + rClassName + " => Rank " + rPermName + " set to '" + tStr[0] + ":" + tStr[1]);
		return true;
	}

	private boolean configClassChange(String cClassName, String cNewName, Player pPlayer) {
		mysqlQuery("UPDATE classranks_classes SET classname = '" + cNewName + "' WHERE classname = '" + cClassName + "';");
		c.msg(pPlayer,"Class " + cClassName + " renamed to " + cNewName);
		return true;
	}

	private boolean configRankAdd(String rClassName, String rPermName, String rValue, Player pPlayer) {
		int cID = mysqlReturnCIDByClassName(rClassName);
		if (cID > -1) {
			String[] tStr = rValue.replace("_", " ").split(":");
			tStr[1] = String.valueOf(Integer.parseInt(tStr[1].substring(1),16));
			
			int rMax = mysqlReturnMaxRank("SELECT * FROM classranks_ranks WHERE cid = '" + cID + "';")+1;
			
			mysqlQuery("INSERT INTO classranks_ranks (cid, oid, permname, dispname, color) VALUES(" + String.valueOf(cID) + ", " + String.valueOf(rMax) + ", '" + rPermName + "', '" + tStr[0] + "', '" + tStr[1] + "');");
			c.msg(pPlayer,"Class " + rClassName + " => Rank #" + String.valueOf(rMax) + " " + rPermName + " added: '" + tStr[0] + ":" + tStr[1]);
			return true;
		}
		c.msg(pPlayer,"Class " + rClassName + " not found!");
		return true;
	}

	private boolean configClassAdd(String cClassName, String cPermName, String cValue, Player pPlayer) {
		if (c.MySQL) {
			mysqlQuery("INSERT INTO classranks_classes (classname) VALUES ('" + cClassName + "');");
		} else {
			int newID = mysqlReturnHighestID("SELECT * FROM classranks_classes WHERE 1;");
			mysqlQuery("INSERT INTO classranks_classes (id, classname) VALUES (" + String.valueOf(++newID) + ", '" + cClassName + "');");
		}
		c.msg(pPlayer,"Class " + cClassName + " added");
		configRankAdd(cClassName, cPermName, cValue, pPlayer);
		
		return true;
	}

	private boolean configRankRemove(String rClassName, String rPermName, Player pPlayer) {
		mysqlQuery("DELETE FROM classranks_ranks WHERE permname = '" + rPermName + "';");
		c.msg(pPlayer,"Class " + rClassName + " => Rank " + rPermName + " deleted");
		return true;
	}

	private boolean configClassRemove(String cClassName, Player pPlayer) {
		int cID = mysqlReturnCIDByClassName(cClassName);
		if (cID > -1) {
			mysqlQuery("DELETE FROM classranks_classes WHERE id = '" + cID + "';");
			mysqlQuery("DELETE FROM classranks_ranks WHERE cid = '" + cID + "';");
			c.msg(pPlayer,"Class " + cClassName + " deleted");
			return true;
		}
		c.msg(pPlayer,"Class " + cClassName + " not found!");
		return true;
	}
	
	/*
	 * Function that loads the database with values given in content.yml
	 */
	@SuppressWarnings("unchecked")
	private void loadDatabase(Player pPlayer) {
        File fConfig = new File(c.getDataFolder(),"content.yml");
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

                        c.log("Created default content.yml", Level.INFO);
                    }
                }
            } catch(Exception e){
            	c.log("Unable to create default content.yml:" + e, Level.INFO);
            }
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
			config.load(fConfig);
		} catch (FileNotFoundException e1) {
			c.log("File not found!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			c.log("IO Exception!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (InvalidConfigurationException e1) {
			c.log("Invalid Configuration!", Level.SEVERE);
			e1.printStackTrace();
			return;
		} catch (Exception e) {
			c.log("Did you update to v0.1.5? - Backup and remove your config!", Level.SEVERE);
			e.printStackTrace();
			return;
		}

        mysqlQuery("DELETE FROM classranks_classes WHERE 1;");
        mysqlQuery("DELETE FROM classranks_ranks WHERE 1;");
		
        Map<String, Object> contents = (Map<String, Object>) config.getConfigurationSection("classes").getValues(true);
        for (String cClass : contents.keySet()) {
        	c.log(cClass, Level.INFO);
        	boolean first = true;
        	Map<String, String> ranks = (Map<String,String>) contents.get(cClass);
        	for (String rRank : ranks.keySet()) {
        		c.log(rRank, Level.INFO);
        		if (first) {
        			// class add
        			configClassAdd(cClass, rRank, ranks.get(rRank), pPlayer);
        			first = false;
        		} else {
        			// rank add
        			configRankAdd(cClass, rRank, ranks.get(rRank), pPlayer);
        		}
        	}
        }
	}
}
