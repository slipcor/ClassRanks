package praxis.slipcor.classranksBP;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/*
 * string formating class
 * 
 * v0.1.6 - cleanup
 * 
 * History:
 * 
 *     v0.1.4.3 - Multiworld "all" support
 *     v0.1.3.4 - Fix: cost parsing
 * 	            - Fix: class change announcement
 *              - Cleanup: static plugin access
 *              - Cleanup: ordering
 * 	   v0.1.3.3 - Possibility to require items for upranking
 * 	   v0.1.3.0 - big rewrite: +SQL, +classadmin
 * 	   v0.1.2.8 - rewritten config, ready for ingame ranks and permissionsbukkit
 * 	   v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 	   v0.1.2.3 - world and player color customizable
 * 	   v0.1.2.0 - renaming for release
 * 
 * @author slipcor
 */

public class CRFormats {
	private ChatColor colPlayer = ChatColor.YELLOW; // Color: Playername
	private ChatColor colWorld = ChatColor.GOLD;   // Color: Worldname
	private final CRPlayers p;
    
    public CRFormats(CRPlayers crp) {
    	p = crp;
    }
    /*
     * Wrap a string in the desired color
     */
	String formatPlayer(String str) {
		String tStr = p.search(str);
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
	 * read an array of ItemStack and format it
	 */
    String formatItemStacks(ItemStack[] itemStacks) {
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
	void setColors(String sColor, String sPlayerCode, String sWorldCode) {
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
