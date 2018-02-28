package org.smartregister.immunization.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.smartregister.domain.Photo;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.domain.jsonmapping.Vaccine;
import org.smartregister.immunization.util.ImageUtils;
import org.smartregister.immunization.view.VaccineCard;
import org.smartregister.immunization.view.VaccineGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import static org.smartregister.util.Utils.getName;
import static org.smartregister.util.Utils.getValue;

/**
 * Created by Jason Rogena - jrogena@ona.io on 22/02/2017.
 */
public class VaccineCardAdapter extends BaseAdapter {
    private static final String TAG = "VaccineCardAdapter";
    private final Context context;
    private HashMap<String, VaccineCard> vaccineCards;
    private final VaccineGroup vaccineGroup;
    private final String type;


    public VaccineCardAdapter(Context context, VaccineGroup vaccineGroup, String type) throws JSONException {
        this.context = context;
        this.vaccineGroup = vaccineGroup;
        vaccineCards = new HashMap<>();
        this.type = type;
    }

    @Override
    public int getCount() {
        return vaccineGroup.getVaccineData().vaccines.size();
    }

    @Override
    public Object getItem(int position) {
        return vaccineCards.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 231231 + position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            Vaccine vaccineData = vaccineGroup.getVaccineData().vaccines
                    .get(position);
            String vaccineName = vaccineData.name;
            if (!vaccineCards.containsKey(vaccineName)) {
                VaccineCard vaccineCard = new VaccineCard(context);
                vaccineCard.setOnVaccineStateChangeListener(vaccineGroup);
                vaccineCard.setOnClickListener(vaccineGroup);
                vaccineCard.getUndoB().setOnClickListener(vaccineGroup);
                vaccineCard.setId((int) getItemId(position));
                VaccineWrapper vaccineWrapper = new VaccineWrapper();
                vaccineWrapper.setId(vaccineGroup.getChildDetails().entityId());
                vaccineWrapper.setGender(vaccineGroup.getChildDetails().getDetails().get("gender"));
                vaccineWrapper.setName(vaccineName);
                vaccineWrapper.setDefaultName(vaccineName);

                String dobString = getValue(vaccineGroup.getChildDetails().getColumnmaps(), "dob", false);
                if (StringUtils.isNotBlank(dobString)) {
                    Calendar dobCalender = Calendar.getInstance();
                    DateTime dateTime = new DateTime(dobString);
                    dobCalender.setTime(dateTime.toDate());
                    dobCalender.add(Calendar.DATE, vaccineGroup.getVaccineData().days_after_birth_due);
                    vaccineWrapper.setVaccineDate(new DateTime(dobCalender.getTime()));
                }

                Photo photo = ImageUtils.profilePhotoByClient(vaccineGroup.getChildDetails());
                vaccineWrapper.setPhoto(photo);

                String zeirId = getValue(vaccineGroup.getChildDetails().getColumnmaps(), "zeir_id", false);
                vaccineWrapper.setPatientNumber(zeirId);

                String firstName = getValue(vaccineGroup.getChildDetails().getColumnmaps(), "first_name", true);
                String lastName = getValue(vaccineGroup.getChildDetails().getColumnmaps(), "last_name", true);
                String childName = getName(firstName, lastName);
                vaccineWrapper.setPatientName(childName.trim());

                vaccineGroup.updateWrapper(vaccineWrapper);
                vaccineGroup.updateWrapperStatus(vaccineWrapper, type);
                vaccineCard.setVaccineWrapper(vaccineWrapper);

                vaccineCards.put(vaccineName, vaccineCard);
            }

            //If last position, toggle RecordAll
            if (position == (getCount() - 1)) {
                vaccineGroup.toggleRecordAllTV();
            }

            return vaccineCards.get(vaccineName);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    public void update(ArrayList<VaccineWrapper> vaccinesToUpdate) {
        if (vaccineCards != null) {
            if (vaccinesToUpdate == null) {// Update all vaccines
                for (VaccineCard curCard : vaccineCards.values()) {
                    if (curCard != null) curCard.updateState();
                }
            } else {// Update just the vaccines specified
                for (VaccineWrapper currWrapper : vaccinesToUpdate) {
                    if (vaccineCards.containsKey(currWrapper.getName())) {
                        vaccineCards.get(currWrapper.getName()).setVaccineWrapper(currWrapper);
                    }
                }
            }
        }
    }

    public ArrayList<VaccineWrapper> getDueVaccines() {
        ArrayList<VaccineWrapper> dueVaccines = new ArrayList<>();
        if (vaccineCards != null) {
            for (VaccineCard curCard : vaccineCards.values()) {
                if (curCard != null && (curCard.getState().equals(VaccineCard.State.DUE)
                        || curCard.getState().equals(VaccineCard.State.OVERDUE))) {
                    dueVaccines.add(curCard.getVaccineWrapper());
                }
            }
        }

        return dueVaccines;
    }

    public ArrayList<VaccineWrapper> getAllVaccineWrappers() {
        ArrayList<VaccineWrapper> allWrappers = new ArrayList<>();
        for (VaccineCard curCard : vaccineCards.values()) {
            allWrappers.add(curCard.getVaccineWrapper());
        }

        return allWrappers;
    }

}
