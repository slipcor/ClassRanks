package praxis.slipcor.classranksBP;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/*
 * string formating class
 * 
 * v0.1.4.3 - Multiworld "all" support
 * 
 * History:
 * 
 *      v0.1.3.4 - Fix: cost parsing
 * 		         - Fix: class change announcement
 *               - Cleanup: static plugin access
 *               - Cleanup: ordering
 * 		v0.1.3.3 - Possibility to require items for upranking
 * 		v0.1.3.0 - big rewrite: +SQL, +classadmin
 * 		v0.1.2.8 - rewritten config, ready for ingame ranks and permissionsbukkit
 * 		v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 		v0.1.2.3 - world and player color customizable
 * 		v0.1.2.0 - renaming for release
 * 
 * @author slipcor
 */

public class CRFormats {
	
    /*
     * Wrap a string in the desired color
     */
	public static String applyColor(String str, ChatColor cColor) {
		if (cColor.equals(CRClasses.colPlayer)) {
			// if we apply the color to a player:
			String tStr = CRPlayers.search(str);
			if (!tStr.equals("")) {
				// if a valid playername: replace
				str = tStr;
			}
		} else {
			if (str.equalsIgnoreCase("all")) {
				str = "all worlds";
			}
		}
		return cColor + str + ChatColor.WHITE;
	}
	
	/*
	 * Get a Chatcolor by colorcode (e.g. '&a')
	 */
	public static ChatColor cColorbyCode(String cCode) {
		return ChatColor.getByCode(Integer.parseInt(cCode.substring(cCode.length()-1),16));
	}
	
	/*
	 * read a string and format it with given color code (e.g. '&e')
	 */
	public static String formatStringByColorCode(String str, String code) {
		// remove the color prefix
		code = code.replace("&", "");
		// calculate the chat color via hex number
		ChatColor cc = ChatColor.getByCode(Integer.parseInt(code, 16));
		// return colored string
		return cc + str + ChatColor.WHITE;
	}
	
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
}
