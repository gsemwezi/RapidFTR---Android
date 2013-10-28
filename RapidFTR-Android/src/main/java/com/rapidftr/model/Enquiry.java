package com.rapidftr.model;

import android.database.Cursor;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.rapidftr.database.Database;
import com.rapidftr.repository.ChildRepository;
import com.rapidftr.utils.RapidFtrDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

import static com.rapidftr.database.Database.EnquiryTableColumn.*;

public class Enquiry extends BaseModel {

    private ArrayList<String> NONE_CRITERIA_FIELDS = new ArrayList<String>();

    public Enquiry() throws JSONException {
        super();
        this.setUniqueId(createUniqueId());
    }

    public Enquiry(String createdBy, String enquirerName, JSONObject criteria) throws JSONException {
        this.setCreatedBy(createdBy);
        this.setEnquirerName(enquirerName);
        this.setCriteria(criteria);
        this.setUniqueId(createUniqueId());
        this.setLastUpdatedAt(RapidFtrDateTime.now().defaultFormat());
    }

    public Enquiry(Cursor cursor) throws JSONException {

        for (Database.EnquiryTableColumn column : Database.EnquiryTableColumn.values()) {
            final int columnIndex = cursor.getColumnIndex(column.getColumnName());
            if (columnIndex < 0) {
                throw new IllegalArgumentException("Column " + column.getColumnName() + " does not exist");
            }
            if (column.getPrimitiveType().equals(Boolean.class)) {
                this.put(column.getColumnName(), cursor.getInt(columnIndex) == 1);
            } else {
                this.put(column.getColumnName(), cursor.getString(columnIndex));
            }
        }
    }

    public Enquiry(String enquiryJSON) throws JSONException {
        super(enquiryJSON);
    }

    public List<Child> getPotentialMatches(ChildRepository childRepository) throws JSONException {
        try{
            JSONArray matchingChildId = new JSONArray(matchingChildIds());
            List<String> matchingChildList = getListOfMatchingChildrenFrom(matchingChildId);

            return childRepository.getChildrenByIds(new ArrayList<String>(matchingChildList));
        } catch (JSONException exception) {
            return new ArrayList<Child>();
        }
    }


    private List<String> getListOfMatchingChildrenFrom(JSONArray matchingChildId) throws JSONException {
        List<String> matchingChildList = new ArrayList<String>();

        for (int i = 0; i < matchingChildId.length(); i++) {
            matchingChildList.add((String) matchingChildId.get(i));
        }
        return matchingChildList;
    }

    private void setColumn(Database.EnquiryTableColumn column, String value) throws JSONException {
        put(column.getColumnName(), value);
    }

    public String getEnquirerName() throws JSONException {
        return getString(enquirer_name.getColumnName());
    }

    public void setEnquirerName(String reporterName) throws JSONException {
        this.setColumn(enquirer_name, reporterName);
    }

    public String getCriteria() throws JSONException {

        JSONObject enquiry_criteria;

        try {
            enquiry_criteria =  new JSONObject(this.get(criteria.getColumnName()).toString());
        } catch (JSONException e) {
            ArrayList<String> keys = getKeys();
            enquiry_criteria = new JSONObject();
            noneCriteriaFields();
            for (String key : keys) {
                if (NONE_CRITERIA_FIELDS.contains(key)) {
                    /* do nothing*/
                } else {
                    enquiry_criteria.put(key, this.getString(key));
                }
            }
        }

        return enquiry_criteria.toString();
    }

    private ArrayList<String> getKeys() {
        Iterator keys = this.keys();
        ArrayList<String> enquiryKeys = new ArrayList<String>();
        while (keys.hasNext()) {
            enquiryKeys.add(keys.next().toString());
        }
        return enquiryKeys;
    }

    private void noneCriteriaFields(){
        NONE_CRITERIA_FIELDS.add("enquirer_name");
        NONE_CRITERIA_FIELDS.add("created_at");
        NONE_CRITERIA_FIELDS.add("created_by");
        NONE_CRITERIA_FIELDS.add("created_organisation");
        NONE_CRITERIA_FIELDS.add("synced");
        NONE_CRITERIA_FIELDS.add("unique_identifier");
    }

    public void setCriteria(JSONObject criteria) throws JSONException {
        this.setColumn(Database.EnquiryTableColumn.criteria, criteria.toString());
    }

    public boolean isValid() {
        String enquirerName;
        try {
            enquirerName = getEnquirerName();
        } catch (JSONException e) {
            return false;
        }
        return null != enquirerName && !"".equals(enquirerName);
    }

    public JSONObject values() throws JSONException {
        Iterable<Object> systemFields = Iterables.transform(Database.EnquiryTableColumn.fields(), new Function<Database.EnquiryTableColumn, Object>() {
            @Override
            public Object apply(Database.EnquiryTableColumn enquiryTableColumn) {
                return enquiryTableColumn.getColumnName();
            }
        });
        List<Object> fields = Lists.newArrayList(systemFields);
        return new JSONObject(this, fields.toArray(new String[fields.size()]));
    }

    public String matchingChildIds() throws JSONException {
        String ids = getString(potential_matches.getColumnName());
        if (ids == null) throw new JSONException("No key " + potential_matches.getColumnName());
        return ids;
    }
}
