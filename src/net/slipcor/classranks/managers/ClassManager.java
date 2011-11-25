package net.slipcor.classranks.managers;

import java.util.ArrayList;

import net.slipcor.classranks.ClassRanks;
import net.slipcor.classranks.core.Class;
import net.slipcor.classranks.core.Rank;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/*
 * class manager class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * @author slipcor
 */

public class ClassManager {
	private static ArrayList<Class> classes = new ArrayList<Class>();
	private static ClassRanks plugin;
	private static DebugManager db;
	
	public ClassManager(ClassRanks cr) {
		plugin = cr;
		db = new DebugManager(cr);
	}
	
	public void add(String sClassName) {
		classes.add(new Class(sClassName));
	}

	public static String getFirstPermNameByClassName(String cString) {
		for (Class c : classes) {
			for (Rank r : c.ranks) {
				if (c.name.equals(cString))
					return r.getPermissionName();
				else
					continue;
			}
		}
		return null;
	}

	public static String getClassNameByPermName(String rank) {
		for (Class c : classes) {
			for (Rank r : c.ranks) {
				if (r.getPermissionName().equals(rank))
					return c.name;
			}
		}
		return null;
	}

	public static String getLastPermNameByPermGroups(ArrayList<String> permGroups) {
		String sPermName = "";
		for (Class c : classes) {
			for (Rank r : c.ranks) {
				db.i(c.name + " => " + r.getPermissionName());
				if (permGroups.contains(r.getPermissionName()))
					sPermName = r.getPermissionName();
			}
		}
		return sPermName;
	}

	public static Rank getRankByPermName(String sPermName) {
		for (Class c : classes) {
			for (Rank r : c.ranks) {
				if (r.getPermissionName().equals(sPermName))
					return r;
			}
		}
		return null;
	}

	public static Rank getNextRank(Rank rank, int rankOffset) {
		return rank.getSuperClass().ranks.get(rank.getSuperClass().ranks.indexOf(rank)+rankOffset);
	}
	
	private static Class getClassbyClassName(String sClassName) {
		for (Class c : classes) {
			if (c.name.equals(sClassName))
				return c;
		}
		return null;
	}
	
	public static String[] validateDispNameColor(String sDispNameColor) {
		db.i("validating display name + color");
		if (!sDispNameColor.contains(":"))
			return null;

		db.i(" - string contains colon");
		String[] result = sDispNameColor.split(":");
		ChatColor cc = null;
		try {
			cc = ChatColor.valueOf(result[1]);
			if (cc == null) {
				cc = ChatColor.getByCode(Integer.parseInt(result[1].substring(1),16));
			}
		} catch (Exception e){
			cc = ChatColor.getByCode(Integer.parseInt(result[1].substring(1),16));
		}

		db.i(" - cc = " + String.valueOf(cc));
		if (cc == null) {
			return null;
		}
		result[1] = cc.name();
		return result;
	}

	public static boolean rankExists(String sRank) {
		for (Class c : classes) {
			for (Rank r : c.ranks) {
				if (r.getPermissionName().equals(sRank))
					return true;
			}
		}
		return false;
	}
	
	public static ArrayList<Class> getClasses() {
		return classes;
	}

	public static boolean configClassRemove(String sClassName, Player pPlayer) {
		Class cClass = getClassbyClassName(sClassName);
		if (cClass != null) {
			classes.remove(cClass);
			plugin.msg(pPlayer, "Class removed: " + sClassName);
			plugin.save_config();
			return true;
		}
		plugin.msg(pPlayer, "Class not found: " + sClassName);
		return true;
	}

	public static boolean configRankRemove(String sClassName, String sPermName, Player pPlayer) {
		Class cClass = getClassbyClassName(sClassName);
		if (cClass != null) {
			Rank rank = getRankByPermName(sPermName);
			if (rank != null) {
				ChatColor cColor = rank.getColor();
				cClass.ranks.remove(rank);
				plugin.msg(pPlayer, "Rank removed: " + cColor + sPermName);
				plugin.save_config();
				return true;
			}
			plugin.msg(pPlayer, "Rank not found: " + sPermName);
			return true;
		}
		plugin.msg(pPlayer, "Class not found: " + sClassName);
		return true;
	}

	public static boolean configClassAdd(String sClassName, String sPermName, String sDispNameColor, Player pPlayer) {
		Class cClass = getClassbyClassName(sClassName);
		if (cClass == null) {
			String[] aDispNameColor = validateDispNameColor(sDispNameColor);
			if (aDispNameColor != null) {
				Class c = new Class(sClassName);
				c.add(sPermName, aDispNameColor[0], ChatColor.valueOf(aDispNameColor[1]));
				classes.add(c);
				if (pPlayer == null)
					db.i("Class added: " + sClassName);
				else
					plugin.msg(pPlayer, "Class added: " + sClassName);
				plugin.save_config();
				return true;
			}
			String[] s = sDispNameColor.split(":");
			if (pPlayer == null)
				db.i("Invalid color code: " + s[1]);
			else
				plugin.msg(pPlayer, "Invalid color code: " + s[1]);
			return true;
		}
		if (pPlayer == null)
			db.i("Class already exists: " + sClassName);
		else
			plugin.msg(pPlayer, "Class already exists: " + sClassName);
		return true;
	}

	public static boolean configRankAdd(String sClassName, String sPermName, String sDispNameColor, Player pPlayer) {
		Class cClass = getClassbyClassName(sClassName);
		if (cClass != null) {
			String[] aDispNameColor = validateDispNameColor(sDispNameColor);
			if (aDispNameColor != null) {
				cClass.ranks.add(new Rank(sPermName, aDispNameColor[0], ChatColor.valueOf(aDispNameColor[1]), cClass));
				if (pPlayer == null)
					db.i("Rank added: " + ChatColor.valueOf(aDispNameColor[1]) + sPermName);
				else
					plugin.msg(pPlayer, "Rank added: " + ChatColor.valueOf(aDispNameColor[1]) + sPermName);
				plugin.save_config();
				return true;
			}
			String[] s = sDispNameColor.split(":");
			if (pPlayer == null)
				db.i("Invalid color code: " + s[1]);
			else
				plugin.msg(pPlayer, "Invalid color code: " + s[1]);
			return true;
		}
		if (pPlayer == null)
			db.i("Class not found: " + sClassName);
		else
			plugin.msg(pPlayer, "Class not found: " + sClassName);
		return true;
	}

	public static boolean configClassChange(String sClassName, String sClassNewName, Player pPlayer) {
		Class cClass = getClassbyClassName(sClassName);
		if (cClass != null) {
			cClass.name = sClassNewName;
			plugin.msg(pPlayer, "Class changed: " + sClassName + " => " + sClassNewName);
			plugin.save_config();
			return true;
		}
		plugin.msg(pPlayer, "Class not found: " + sClassName);
		return true;
	}

	public static boolean configRankChange(String sClassName, String sPermName, String sDispNameColor, Player pPlayer) {
		Class cClass = getClassbyClassName(sClassName);
		if (cClass != null) {
			Rank rank = getRankByPermName(sPermName);
			if (rank != null) {
				String[] aDispNameColor = validateDispNameColor(sDispNameColor);
				if (aDispNameColor != null) {
					rank.setDispName(aDispNameColor[0]);
					rank.setColor(ChatColor.valueOf(aDispNameColor[1]));
					plugin.msg(pPlayer, "Rank updated: " + ChatColor.valueOf(aDispNameColor[1]) + sPermName);
					plugin.save_config();
					return true;
				}
				String[] s = sDispNameColor.split(":");
				plugin.msg(pPlayer, "Invalid color code: " + s[1]);
				return true;
			}
			plugin.msg(pPlayer, "Rank not found: " + sPermName);
		}
		plugin.msg(pPlayer, "Class not found: " + sClassName);
		return true;
	}
}
