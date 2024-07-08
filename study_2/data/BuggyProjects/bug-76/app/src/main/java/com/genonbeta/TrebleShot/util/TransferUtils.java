package com.genonbeta.TrebleShot.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;

import java.util.List;

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
public class TransferUtils
{
    public static final String TAG = TransferUtils.class.getSimpleName();

    public static final int TASK_START_TRANSFER_WITH_OVERVIEW = 1;

    public static void changeConnection(FragmentActivity activity, final AccessDatabase database, final TransferGroup group, final NetworkDevice device, final ConnectionUpdatedListener listener)
    {
        new ConnectionChooserDialog(activity, device, new OnDeviceSelectedListener()
        {
            @Override
            public void onDeviceSelected(NetworkDevice.Connection connection, List<NetworkDevice.Connection> connectionList)
            {
                TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device, connection);

                database.publish(assignee);

                if (listener != null)
                    listener.onConnectionUpdated(connection, assignee);
            }
        }).show();
    }

    @SuppressLint("DefaultLocale")
    public static long createUniqueTransferId(long groupId, String deviceId, TransferObject.Type type)
    {
        return String.format("%d_%s_%s", groupId, deviceId, type).hashCode();
    }

    public static SQLQuery.Select createTransferSelection(long groupId, String deviceId)
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(String.format("%s = ? AND %s = ?",
                        AccessDatabase.FIELD_TRANSFER_GROUPID,
                        AccessDatabase.FIELD_TRANSFER_DEVICEID),
                        String.valueOf(groupId), deviceId);
    }

    public static SQLQuery.Select createTransferSelection(long groupId, String deviceId, TransferObject.Flag flag, boolean equals)
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(String.format("%s = ? AND %s = ? AND %s " + (equals ? "=" : "!=") + " ?",
                        AccessDatabase.FIELD_TRANSFER_GROUPID,
                        AccessDatabase.FIELD_TRANSFER_DEVICEID,
                        AccessDatabase.FIELD_TRANSFER_FLAG),
                        String.valueOf(groupId), deviceId, flag.toString());
    }

    public static ShowingAssignee getFirstAssignee(AccessDatabase database, TransferGroup group)
    {
        SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(group.groupId));

        List<ShowingAssignee> assignees = database
                .castQuery(select, ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<ShowingAssignee>()
                {
                    @Override
                    public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, ShowingAssignee object)
                    {
                        object.device = new NetworkDevice(object.deviceId);
                        object.connection = new NetworkDevice.Connection(object);

                        try {
                            db.reconstruct(object.device);
                            db.reconstruct(object.connection);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        return assignees.size() == 0 ? null : assignees.get(0);
    }

    public static ShowingAssignee getFirstAssignee(SnackbarSupport snackbar, AccessDatabase database, TransferGroup group)
    {
        ShowingAssignee assignee = getFirstAssignee(database, group);

        if (assignee == null) {
            snackbar.createSnackbar(R.string.mesg_noReceiverOrSender)
                    .show();

            return null;
        }

        return assignee;
    }

    public static TransferObject fetchValidIncomingTransfer(Context context, long groupId, String deviceId)
    {
        CursorItem receiverInstance = AppUtils.getDatabase(context).getFirstFromTable(new SQLQuery
                .Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_DEVICEID + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_FLAG + "=?",
                        TransferObject.Type.INCOMING.toString(),
                        String.valueOf(groupId),
                        deviceId,
                        TransferObject.Flag.PENDING.toString())
                .setOrderBy(String.format("`%s` ASC, `%s` ASC",
                        AccessDatabase.FIELD_TRANSFER_DIRECTORY,
                        AccessDatabase.FIELD_TRANSFER_NAME)));

        return receiverInstance == null
                ? null
                : new TransferObject(receiverInstance);
    }

    public static List<ShowingAssignee> loadAssigneeList(SQLiteDatabase database, long groupId)
    {
        SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId));

        return database.castQuery(select, ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<ShowingAssignee>()
        {
            @Override
            public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, ShowingAssignee object)
            {
                object.device = new NetworkDevice(object.deviceId);
                object.connection = new NetworkDevice.Connection(object);

                try {
                    db.reconstruct(object.device);
                } catch (Exception e) {
                    // Nope
                }

                try {
                    db.reconstruct(object.connection);
                } catch (Exception e) {
                    // Nope
                }
            }
        });
    }

    public static void pauseTransfer(Context context, TransferGroup group, @Nullable TransferGroup.Assignee assignee)
    {
        pauseTransfer(context, group.groupId, assignee == null ? null : assignee.deviceId);
    }

    public static void pauseTransfer(Context context, long groupId, @Nullable String deviceId)
    {
        Intent intent = new Intent(context, CommunicationService.class)
                .setAction(CommunicationService.ACTION_CANCEL_JOB)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, groupId)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, deviceId);

        AppUtils.startForegroundService(context, intent);
    }

    public static void recoverIncomingInterruptions(Context context, long groupId)
    {
        ContentValues contentValues = new ContentValues();

        contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransferObject.Flag.PENDING.toString());

        AppUtils.getDatabase(context).update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_FLAG + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
                        String.valueOf(groupId),
                        TransferObject.Flag.INTERRUPTED.toString(),
                        TransferObject.Type.INCOMING.toString()), contentValues);
    }

    public static void startTransferWithTest(final Activity activity, final TransferGroup group, final TransferGroup.Assignee assignee)
    {
        final Context context = activity.getApplicationContext();

        new WorkerService.RunningTask()
        {
            @Override
            protected void onRun()
            {
                if (activity.isFinishing())
                    return;

                if (fetchValidIncomingTransfer(activity, group.groupId, assignee.deviceId) == null) {
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                            builder.setMessage(R.string.mesg_noPendingTransferObjectExists);
                            builder.setNegativeButton(R.string.butn_close, null);
                            builder.setPositiveButton(R.string.butn_retryReceiving, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    recoverIncomingInterruptions(activity, group.groupId);
                                    startTransferWithTest(activity, group, assignee);
                                }
                            });

                            builder.show();
                        }
                    });
                } else {
                    final String savingPath = FileUtils.getSavePath(activity, group).getUri().toString();

                    if (!savingPath.equals(group.savePath)) {
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                                builder.setMessage(context.getString(R.string.mesg_notSavingToChosenLocation, FileUtils.getReadableUri(group.savePath)));
                                builder.setNegativeButton(R.string.butn_close, null);

                                builder.setPositiveButton(R.string.butn_saveAnyway, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        startTransfer(activity, group, assignee);
                                    }
                                });

                                builder.show();
                            }
                        });
                    } else
                        startTransfer(activity, group, assignee);
                }
            }
        }.setTitle(activity.getString(R.string.mesg_completing)).run(activity);
    }

    public static void startTransfer(final Activity activity, final TransferGroup group, final TransferGroup.Assignee assignee)
    {
        if (activity != null && !activity.isFinishing())
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    try {
                        NetworkDevice networkDevice = new NetworkDevice(assignee.deviceId);

                        AppUtils.getDatabase(activity)
                                .reconstruct(networkDevice);

                        new EstablishConnectionDialog(activity, networkDevice, new OnDeviceSelectedListener()
                        {
                            @Override
                            public void onDeviceSelected(NetworkDevice.Connection connection, List<NetworkDevice.Connection> availableInterfaces)
                            {
                                if (!assignee.connectionAdapter.equals(connection.adapterName)) {
                                    assignee.connectionAdapter = connection.adapterName;

                                    AppUtils.getDatabase(activity)
                                            .publish(assignee);
                                }

                                AppUtils.startForegroundService(activity, new Intent(activity, CommunicationService.class)
                                        .setAction(CommunicationService.ACTION_SEAMLESS_RECEIVE)
                                        .putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId)
                                        .putExtra(CommunicationService.EXTRA_DEVICE_ID, assignee.deviceId));
                            }
                        }).show();
                    } catch (Exception e) {
                        new AlertDialog.Builder(activity)
                                .setMessage(R.string.mesg_somethingWentWrong)
                                .setNegativeButton(R.string.butn_cancel, null)
                                .setPositiveButton(R.string.butn_retry, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        startTransfer(activity, group, assignee);
                                    }
                                })
                                .show();
                    }
                }
            });
    }

    public interface ConnectionUpdatedListener
    {
        void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee);
    }
}
