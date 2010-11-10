/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;

/**
 *
 * @author Tom Tasche <tomtasche@gmail.com>
 *
 */
public class LinkContactPlugin extends BroadcastReceiver {
    static final String IDENTIFIER = "linkContact"; //$NON-NLS-1$

    @Override
    @SuppressWarnings("nls")
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Call Astrid!", "TomTasche",
                "Don't forget to call Astrid on time.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }
}