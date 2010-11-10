package org.weloveastrid.contacts;

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
    private String name;
    private final LinkContactService contactService = LinkContactService.getInstance();

    public LinkContactControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);

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
        name = contactService.getContact(task.getId());
        if(name != null)
            selectContact.setText(name);
    }

    @Override
    public String writeToModel(Task task) {
        if (name == null) return null;

        contactService.synchronizeContact(task.getId(), name);
        return name;
    }
}