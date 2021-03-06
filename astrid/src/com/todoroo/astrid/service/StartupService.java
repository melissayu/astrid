package com.todoroo.astrid.service;

import java.io.File;
import java.util.List;

import org.weloveastrid.rmilk.MilkUtilities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.ExceptionService.TodorooUncaughtExceptionHandler;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.backup.BackupConstants;
import com.todoroo.astrid.backup.BackupService;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.reminders.ReminderStartupReceiver;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.widget.TasksWidget.UpdateService;

/**
 * Service which handles jobs that need to be run when Astrid starts up.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class StartupService {

    static {
        AstridDependencyInjector.initialize();
    }

    public StartupService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- application startup

    @Autowired
    ExceptionService exceptionService;

    @Autowired
    UpgradeService upgradeService;

    @Autowired
    TaskService taskService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    Database database;

    /**
     * bit to prevent multiple initializations
     */
    private static boolean hasStartedUp = false;

    /** Called when this application is started up */
    public synchronized void onStartupApplication(final Context context) {
        if(hasStartedUp)
            return;

        // set uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new TodorooUncaughtExceptionHandler());

        // sets up context manager
        ContextManager.setContext(context);

        // read current version
        int latestSetVersion = AstridPreferences.getCurrentVersion();
        int version = 0;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(Constants.PACKAGE, PackageManager.GET_META_DATA);
            version = pi.versionCode;
        } catch (Exception e) {
            exceptionService.reportError("astrid-startup-package-read", e); //$NON-NLS-1$
        }

        Log.i("astrid", "Astrid Startup. " + latestSetVersion + //$NON-NLS-1$ //$NON-NLS-2$
                " => " + version); //$NON-NLS-1$

        databaseRestoreIfEmpty(context);

        // invoke upgrade service
        boolean justUpgraded = latestSetVersion != version;
        if(justUpgraded && version > 0) {
            upgradeService.performUpgrade(context, latestSetVersion);
            AstridPreferences.setCurrentVersion(version);
        }
        if(latestSetVersion == 0) {
            onFirstTime();
        }

        upgradeService.performSecondaryUpgrade(context);

        // perform startup activities in a background thread
        new Thread(new Runnable() {
            public void run() {
                // start widget updating alarm
                AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(context, UpdateService.class);
                PendingIntent pendingIntent = PendingIntent.getService(context,
                        0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                am.setInexactRepeating(AlarmManager.RTC, 0,
                        Constants.WIDGET_UPDATE_INTERVAL, pendingIntent);

                database.openForWriting();
                taskService.cleanup();

                // schedule alarms
                ReminderStartupReceiver.startReminderSchedulingService(context);

                // if sync ongoing flag was set, clear it
                ProducteevUtilities.INSTANCE.stopOngoing();
                MilkUtilities.INSTANCE.stopOngoing();

                BackupService.scheduleService(context);

                // get and display update messages
                new UpdateMessageService().processUpdates(context);
            }
        }).start();

        AstridPreferences.setPreferenceDefaults();

        // check for task killers
        if(!Constants.OEM)
            showTaskKillerHelp(context);

        hasStartedUp = true;
    }

    /**
     * Create tasks for first time users
     */
    private void onFirstTime() {
        Resources r = ContextManager.getResources();

        addIntroTask(r, R.string.intro_task_1_summary, R.string.intro_task_1_note);
        addIntroTask(r, R.string.intro_task_2_summary, R.string.intro_task_2_note);
        addIntroTask(r, R.string.intro_task_3_summary, R.string.intro_task_3_note);
    }

    private void addIntroTask(Resources r, int summary, int note) {
        Task task = new Task();
        task.setValue(Task.TITLE, r.getString(summary));
        task.setValue(Task.DETAILS, r.getString(R.string.intro_click_prompt));
        task.setValue(Task.DETAILS_DATE, 2*DateUtilities.now());
        task.setValue(Task.NOTES, r.getString(note));
        taskService.save(task);
    }

    /**
     * If database exists, no tasks but metadata, and a backup file exists, restore it
     */
    private void databaseRestoreIfEmpty(Context context) {
        try {
            if(AstridPreferences.getCurrentVersion() >= UpgradeService.V3_0_0 &&
                    !context.getDatabasePath(database.getName()).exists()) {
                // we didn't have a database! restore latest file
                File directory = BackupConstants.defaultExportDirectory();
                if(!directory.exists())
                    return;
                File[] children = directory.listFiles();
                AndroidUtilities.sortFilesByDateDesc(children);
                if(children.length > 0) {
                    StatisticsService.sessionStart(context);
                    TasksXmlImporter.importTasks(context, children[0].getAbsolutePath(), null);
                    StatisticsService.reportEvent("lost-tasks-restored"); //$NON-NLS-1$
                }
            }
        } catch (Exception e) {
            Log.w("astrid-database-restore", e); //$NON-NLS-1$
        }
    }

    private static final String P_TASK_KILLER_HELP = "taskkiller"; //$NON-NLS-1$

    /**
     * Show task killer helper
     * @param context
     */
    private static void showTaskKillerHelp(final Context context) {
        if(!Preferences.getBoolean(P_TASK_KILLER_HELP, false))
            return;

        // search for task killers. if they exist, show the help!
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm
                .getInstalledPackages(PackageManager.GET_PERMISSIONS);
        outer: for (PackageInfo app : apps) {
            if(app == null || app.requestedPermissions == null)
                continue;
            if(app.packageName.startsWith("com.android")) //$NON-NLS-1$
                continue;
            for (String permission : app.requestedPermissions) {
                if (Manifest.permission.RESTART_PACKAGES.equals(permission)) {
                    CharSequence appName = app.applicationInfo.loadLabel(pm);
                    OnClickListener listener = new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0,
                                int arg1) {
                            Preferences.setBoolean(P_TASK_KILLER_HELP, true);
                        }
                    };

                    new AlertDialog.Builder(context)
                    .setTitle(R.string.DLG_information_title)
                    .setMessage(context.getString(R.string.task_killer_help,
                            appName))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.task_killer_help_ok, listener)
                    .show();

                    break outer;
                }
            }
        }
    }
}
