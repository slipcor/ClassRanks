package net.slipcor.classranks.managers;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * String formating class
 * 
 * @version v0.3.0 
 * 
 * @author slipcor
 */

public class FormatManager {
	private ChatColor colPlayer = ChatColor.YELLOW; // Color: Playername
	private ChatColor colWorld = ChatColor.GOLD;   // Color: Worldname

    /*
     * Wrap a string in the desired color
     */
	String formatPlayer(String str) {
		String tStr = PlayerManager.search(str);
		if (!tStr.equals("")) {
			// if a valid playername: replace
			str = tStr;
		}
		return colPlayer + str + ChatColor.WHITE;
	}
	
    /*
     * Wrap a string in the desired color
     */
	String formatWorld(String str) {
		if (str.equalsIgnoreCase("all")) {
			str = "all worlds";
		}
		return colWorld + str + ChatColor.WHITE;
	}
	
	/*
	 * read a string and format it with given color code (e.g. '&e')
	 */
	String formatStringByColorCode(String str, String code) {
		// remove the color prefix
		code = code.replace("&", "");
		ChatColor cc = formatColor(code);
		// return colored string
		return cc + str + ChatColor.WHITE;
	}
	
	/*
	 * read a string array and return a readable string
	 */
	public static String formatStringArray(String[] s) {
		if (s == null)
			return "NULL";
		String result = "";
		for (int i=0; i<s.length; i++) {
			result = result + (result.equals("")?"":",") + s[i];
		}
		return result;
	}
	
	/*
	 * read an array of ItemStack and format it
	 */
    public static String formatItemStacks(ItemStack[] itemStacks) {
		String result = "";
    	for (int i=0;i<itemStacks.length;i++) {
    		if (!result.equals("")) {
    			result += ", ";
    		}
    		try {
    			result += String.valueOf(itemStacks[i].getAmount()) + "x " + itemStacks[i].getType().name();
    		} catch (Exception e) {
    			continue;
    		}
    	}
		return result;
	}
    
	public static ItemStack[] getItemStacksFromStringList(List<String> stringList) {
		String result = "";
		for (String item : stringList) {
			result += result.equals("")?item:","+item;
		}
		return getItemStacksFromCommaString(result);
	}

	public static ItemStack[] getItemStacksFromCommaString (String string) {
		String[] arrStacks = string.split(",");
		ItemStack[] result = new ItemStack[arrStacks.length];
		int i = 0;
		for (String stack : arrStacks) {
			if (stack.contains(":")) {
				String[] vars = stack.split(":");
				if (vars.length == 2) {
					int iAmount = 1;
					try {
						iAmount = Integer.parseInt(vars[1]);
					} catch (Exception e) {
						Bukkit.getLogger().warning("unrecognized amount: "+vars[1]);
					}
					// ITEM:COUNT
					try {
						int iType = Integer.parseInt(stack);
						result[i++] = new ItemStack(iType, iAmount);
					} catch (Exception e) {
						result[i++] = new ItemStack(Material.valueOf(vars[0]), iAmount);
					}
				} else {
					// ITEM:DATA:COUNT

					int iAmount = 1;
					try {
						iAmount = Integer.parseInt(vars[2]);
					} catch (Exception e) {
						Bukkit.getLogger().warning("unrecognized amount: "+vars[2]);
					}
					short sDamage = 0;
					try {
						sDamage = Short.parseShort(vars[1]);
					} catch (Exception e) {
						Bukkit.getLogger().warning("unrecognized damage: "+vars[1]);
					}
					try {
						int iType = Integer.parseInt(stack);
						result[i++] = new ItemStack(iType, iAmount, sDamage);
					} catch (Exception e) {
						result[i++] = new ItemStack(Material.valueOf(vars[0]), iAmount, sDamage);
					}
				}
			} else {
				try {
					int iType = Integer.parseInt(stack);
					result[i++] = new ItemStack(iType, 1);
				} catch (Exception e) {
					result[i++] = new ItemStack(Material.valueOf(stack),1);
				}
			}
		}
		return result;
	}
    
    /*
     * store the config colors into our private values
     */
	public void setColors(String sColor, String sPlayerCode, String sWorldCode) {
		colPlayer = formatColor(sPlayerCode);
		colWorld = formatColor(sWorldCode);
	}
	
	/*
	 * Get a Chatcolor by colorcode (e.g. '&a')
	 */
	public static ChatColor formatColor(String cCode) {
		// remove the color prefix
		cCode = cCode.replace("&", "");
		// calculate the chat color via hex number
		return ChatColor.getByChar(cCode);
	}
}
