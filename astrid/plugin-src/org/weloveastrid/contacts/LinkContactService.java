package org.weloveastrid.contacts;

import java.util.ArrayList;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.service.MetadataService;

public class LinkContactService {
    // --- public constants

    /** Metadata key for tag data */
    public static final String KEY = "linkedcontact"; //$NON-NLS-1$

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

    public LinkContactService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public String getContact(long taskId) {
        Query query = Query.select(CONTACT).where(Criterion.and(MetadataCriteria.withKey(KEY),
                MetadataCriteria.byTask(taskId)));
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        try {
            if(cursor.moveToFirst())
                return cursor.get(LinkContactService.CONTACT);
            return null;
        } finally {
            cursor.close();
        }
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

}