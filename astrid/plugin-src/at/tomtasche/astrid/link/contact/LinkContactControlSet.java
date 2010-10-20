package at.tomtasche.astrid.link.contact;

import android.app.Activity;
import android.content.Intent;
import android.provider.Contacts.People;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;

@SuppressWarnings("deprecation")
public class LinkContactControlSet implements TaskEditControlSet {
    private final Button selectContact;
    private final Activity activity;
    private String name;

    public LinkContactControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.link_contact_control, parent, true);

        selectContact = (Button) activity.findViewById(R.id.select_contact);
        selectContact.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, People.CONTENT_URI);
                activity.startActivityForResult(contactPickerIntent, TaskEditActivity.REQUEST_CODE_CALL_ASTRID);
            }
        });
    }

    public void contactSelected(Intent data) {
        name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        if (name != null) {
            selectContact.setText(name);
        }
    }

    @Override
    public void readFromTask(Task task) {
        String temp = task.getValue(Task.LINKED_CONTACT);
        if (selectContact.getText().equals(activity.getResources().getString(R.string.gcal_TEA_showCalendar_label)) && !"".equals(temp)) { //$NON-NLS-1$
            selectContact.setText(temp);
            name = temp;
        }
    }

    @Override
    public String writeToModel(Task task) {
        task.setValue(Task.LINKED_CONTACT, name);
        return name;
    }
}