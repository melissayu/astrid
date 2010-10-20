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
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for linked contacts
 *
 * @author Tom Tasche <tomtasche@gmail.com>
 *
 */
@SuppressWarnings("deprecation")
public class LinkContactActionExposer extends BroadcastReceiver {

    private static final String CONTACT_ACTION = "at.tomtasche.astrid.link.contact.CONTACT_BUTTON"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        if(!PluginServices.getAddOnService().hasPowerPack())
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.LINKED_CONTACT);

        // was part of a broadcast for actions
        if(AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
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
        } else if(CONTACT_ACTION.equals(intent.getAction())) {
            Cursor cursor = context.getContentResolver().query(People.CONTENT_URI, new String[] {People._ID}, People.DISPLAY_NAME + "=" + "'" + task.getValue(Task.LINKED_CONTACT) + "'", null, null);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

            if (cursor == null || !cursor.moveToFirst()) return;

            Intent contactIntent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(People.CONTENT_URI, String.valueOf(cursor.getLong(0))));
            contactIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(contactIntent);

            cursor.close();
        }
    }

}
