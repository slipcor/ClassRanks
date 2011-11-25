package net.slipcor.classranks.managers;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/*
 * String formating class
 * 
 * v0.2.0 - mayor rewrite; no SQL; multiPermissions
 * 
 * History:
 * 
 *     v0.1.6 - cleanup
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
		ChatColor cc = cColorbyAmpedCode(code);
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
    
    /*
     * store the config colors into our private values
     */
	public void setColors(String sColor, String sPlayerCode, String sWorldCode) {
		colPlayer = cColorbyAmpedCode(sPlayerCode);
		colWorld = cColorbyAmpedCode(sWorldCode);
	}
	
	/*
	 * Get a Chatcolor by colorcode (e.g. '&a')
	 */
	private ChatColor cColorbyAmpedCode(String cCode) {
		// remove the color prefix
		cCode = cCode.replace("&", "");
		// calculate the chat color via hex number
		return ChatColor.getByCode(Integer.parseInt(cCode,16));
	}
}
