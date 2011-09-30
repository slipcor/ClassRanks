package praxis.slipcor.classranksBP;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

import praxis.classranks.register.payment.Methods;
import praxis.slipcor.classranksBP.ClassRanks;

/*
 * server listener class
 * 
 * v0.1.4.3 - getServer NPE Fix
 * 
 * History:
 * 
 * 		v0.1.4.0 - Register API
 *      v0.1.3.4 - Fix: cost parsing
 *               - Fix: class change announcement
 *               - Cleanup: static plugin access
 *               - Cleanup: ordering
 * 		v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 		v0.1.2.0 - renaming for release
 * 
 * @author slipcor
 */
public class CRServerListener extends ServerListener {
	private Methods methods = null;

    public CRServerListener() {
    	this.methods = new Methods();
    }

    @SuppressWarnings("static-access")
    @Override
    public void onPluginDisable(PluginDisableEvent event) {
        // Check to see if the plugin thats being disabled is the one we are using
        if (this.methods != null && this.methods.hasMethod()) {
            Boolean check = this.methods.checkDisabled(event.getPlugin());

            if(check) {
                ClassRanks.method = null;
                ClassRanks.log("</3 eConomy",Level.INFO);
            }
        }
    }

    @SuppressWarnings("static-access")
	@Override
    public void onPluginEnable(PluginEnableEvent event) {
        // Check to see if we need a payment method
        if (!this.methods.hasMethod()) {
            if(this.methods.setMethod(Bukkit.getServer().getPluginManager())) {
                ClassRanks.method = this.methods.getMethod();
                ClassRanks.log("<3 " + ClassRanks.method.getName() + " version: " + ClassRanks.method.getVersion(),Level.INFO);
            } else {
		       	ClassRanks.log("</3 eConomy",Level.INFO);
            }
        }
    }
}