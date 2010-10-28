package at.tomtasche.astrid.link.contact;

import java.util.ArrayList;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Flags;

public class LinkContactService {
    // --- public constants

    /** Metadata key for tag data */
    public static final String KEY = "linkcontact-contact";

    /** Property for reading tag values */
    public static final StringProperty CONTACT = Metadata.VALUE1;

    // --- singleton

    private static LinkContactService instance = null;

    public static synchronized LinkContactService getInstance() {
        if(instance == null)
            instance = new LinkContactService();
        return instance;
    }

    // --- implementation details

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private TaskService taskService;

    public LinkContactService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public TodorooCursor<Metadata> getContact(long taskId) {
        Query query = Query.select(CONTACT).where(Criterion.and(MetadataCriteria.withKey(KEY),
                MetadataCriteria.byTask(taskId)));

        if (query == null) return null;

        return metadataDao.query(query);
    }

    public boolean synchronizeContact(long taskId, String contact) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        Metadata item = new Metadata();
        item.setValue(Metadata.KEY, KEY);
        item.setValue(CONTACT, contact);
        metadata.add(item);

        return service.synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(KEY)) > 0;
    }

    public int delete(String contact) {
        invalidateTaskCache(contact);
        return PluginServices.getMetadataService().deleteWhere(tagEq(contact, Criterion.all));
    }

    private static Criterion tagEq(String contact, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(KEY), CONTACT.eq(contact),
                additionalCriterion);
    }

    private void invalidateTaskCache(String contact) {
        taskService.clearDetails(Task.ID.in(rowsWithTag(contact, Task.ID)));
        Flags.set(Flags.REFRESH);
    }

    private Query rowsWithTag(String contact, Field... projections) {
        return Query.select(projections).from(Metadata.TABLE).where(Metadata.VALUE1.eq(contact));
    }
}