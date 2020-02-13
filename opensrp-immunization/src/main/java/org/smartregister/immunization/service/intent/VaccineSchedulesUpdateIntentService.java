package org.smartregister.immunization.service.intent;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.joda.time.DateTime;
import org.smartregister.CoreLibrary;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.immunization.domain.ServiceSchedule;
import org.smartregister.immunization.domain.VaccinationClient;
import org.smartregister.immunization.domain.VaccineSchedule;
import org.smartregister.immunization.util.IMConstants;
import org.smartregister.immunization.util.IMDatabaseConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2020-02-13
 */

public class VaccineSchedulesUpdateIntentService extends IntentService {

    private static final String serviceName = "VaccineSchedulesUpdateIntentService";

    public VaccineSchedulesUpdateIntentService() {
        super(serviceName);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        String tableName = null;

        if (intent != null) {
            tableName = intent.getStringExtra(IMConstants.IntentKey.VaccineScheduleUpdateIntentService.CLIENT_TABLE_NAME);
        }

        if (tableName == null) {
            tableName = "ec_client";
        }

        Timber.i("%s has started running", serviceName);
        sendLocalBroadcast(IMConstants.BroadcastAction.VaccineScheduleUpdate.SERVICE_STARTED);

        int i = 0;
        ArrayList<VaccinationClient> vaccinationClients;
        do {
            vaccinationClients = getClients(tableName, i);

            for(VaccinationClient vaccinationClient : vaccinationClients) {
                VaccineSchedule.updateOfflineAlerts(vaccinationClient.getBaseEntityId(), vaccinationClient.getBirthDateTime(), "child");
                ServiceSchedule.updateOfflineAlerts(vaccinationClient.getBaseEntityId(), vaccinationClient.getBirthDateTime());
            }

        } while(!vaccinationClients.isEmpty());

        Timber.i("%s has finished running successfully", serviceName);
        sendLocalBroadcast(IMConstants.BroadcastAction.VaccineScheduleUpdate.SERVICE_FINISHED);
    }

    @NonNull
    protected ArrayList<VaccinationClient> getClients(@NonNull String tableName, int batchSize, int page) {
        String sql = String.format(Locale.ENGLISH, "SELECT %s, %s FROM %s LIMIT %d, %d", IMDatabaseConstants.Client.ID, IMDatabaseConstants.Client.DOB, tableName, batchSize, (page * batchSize));

        List<CommonPersonObject> personClients = CoreLibrary.getInstance().context().commonrepository(tableName)
                .customQuery(sql, new String[]{}, tableName);

        ArrayList<VaccinationClient> vaccinationClients = new ArrayList<>();
        for (CommonPersonObject commonPersonObject: personClients) {
            VaccinationClient vaccinationClient = new VaccinationClient();
            vaccinationClient.setBaseEntityId(commonPersonObject.getColumnmaps().get(IMDatabaseConstants.Client.ID));
            String dobString = commonPersonObject.getColumnmaps().get(IMDatabaseConstants.Client.DOB);

            if (!TextUtils.isEmpty(dobString)) {
                DateTime dob = new DateTime(dobString);
                vaccinationClient.setBirthDateTime(dob);
                vaccinationClients.add(vaccinationClient);
            } else {
                Timber.e(new Exception(), "VaccinationClient [%s] was skipped and will not have their vaccination schedules updated because they do not have a DOB", vaccinationClient.getBaseEntityId());
            }
        }

        return vaccinationClients;
    }

    @NonNull
    protected ArrayList<VaccinationClient> getClients(@NonNull String tableName, int page) {
        return getClients(tableName, 25, page);
    }

    protected void sendLocalBroadcast(@NonNull String action) {
        Intent broadcastIntent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }
}