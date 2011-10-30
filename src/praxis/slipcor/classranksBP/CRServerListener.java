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
 * v0.1.6 - cleanup
 * 
 * History:
 * 
 *     v0.1.4.3 - getServer NPE Fix
 * 	   v0.1.4.0 - Register API
 *     v0.1.3.4 - Fix: cost parsing
 *              - Fix: class change announcement
 *              - Cleanup: static plugin access
 *              - Cleanup: ordering
 * 	   v0.1.2.7 - consistency tweaks, removed debugging, username autocomplete
 * 	   v0.1.2.0 - renaming for release
 * 
 * @author slipcor
 */
public class CRServerListener extends ServerListener {
	private Methods methods = null;
	private final ClassRanks c;

    public CRServerListener(ClassRanks plugin) {
    	c = plugin;
    	this.methods = new Methods();
    }

    @SuppressWarnings("static-access")
    @Override
    public void onPluginDisable(PluginDisableEvent event) {
        // Check to see if the plugin thats being disabled is the one we are using
        if (this.methods != null && this.methods.hasMethod()) {
            Boolean check = this.methods.checkDisabled(event.getPlugin());

            if(check) {
                c.method = null;
                c.log("</3 eConomy",Level.INFO);
            }
        }
    }

    @SuppressWarnings("static-access")
	@Override
    public void onPluginEnable(PluginEnableEvent event) {
        // Check to see if we need a payment method
        if (!this.methods.hasMethod()) {
            if(this.methods.setMethod(Bukkit.getServer().getPluginManager())) {
                c.method = this.methods.getMethod();
                c.log("<3 " + c.method.getName() + " version: " + c.method.getVersion(),Level.INFO);
            } else {
		       	c.log("</3 eConomy",Level.INFO);
            }
        }
    }
}