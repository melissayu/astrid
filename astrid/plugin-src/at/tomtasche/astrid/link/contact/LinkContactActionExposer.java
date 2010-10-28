/**
 * See the file "LICENSE" for the full license governing this code.
 */
package at.tomtasche.astrid.link.contact;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts.People;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;

/**
 * Exposes {@link TaskDecoration} for linked contacts
 *
 * @author Tom Tasche <tomtasche@gmail.com>
 *
 */
@SuppressWarnings("deprecation")
public class LinkContactActionExposer extends BroadcastReceiver {

    private static final String CONTACT_ACTION = "at.tomtasche.astrid.link.contact.CONTACT_BUTTON"; //$NON-NLS-1$

    private String name;

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        if(!PluginServices.getAddOnService().hasPowerPack())
            return;

        // check if there's already contact selected
        TodorooCursor<Metadata> cursor = LinkContactService.getInstance().getContact(taskId);
        if (cursor == null) return;

        if (cursor.moveToFirst()) {
            name = cursor.get(LinkContactService.CONTACT);

            // no? then do nothing.
            if (name == null) return;

            cursor.close();
        } else {
            cursor.close();
            return;
        }

        // seems like we found a selected contact in metadata. go on. :)

        // was part of a broadcast for actions
        if (AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
            Intent newIntent = new Intent(CONTACT_ACTION);
            newIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            TaskAction action = new TaskAction(context.getString(R.string.link_button_label),
                    PendingIntent.getBroadcast(context, (int)taskId, newIntent, 0));

            // transmit
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, LinkContactPlugin.IDENTIFIER);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        } else if (CONTACT_ACTION.equals(intent.getAction())) {
            // fetch the contact's ID
            Cursor contactCursor = context.getContentResolver().query(People.CONTENT_URI, new String[] {People._ID}, People.DISPLAY_NAME + "=" + "'" + name + "'", null, null);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

            if (contactCursor == null || !contactCursor.moveToFirst()) return;

            // open the contact's activity
            Intent contactIntent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(People.CONTENT_URI, String.valueOf(contactCursor.getLong(0))));
            contactIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(contactIntent);

            contactCursor.close();
        }
    }

}
