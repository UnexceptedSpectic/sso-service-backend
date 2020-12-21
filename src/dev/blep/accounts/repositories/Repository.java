package dev.blep.accounts.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

public class Repository {

    public static boolean fieldValueExists(MongoCollection<Document> collection, String filterField, String filterValue) {
        return collection.countDocuments(
                Filters.eq(filterField, filterValue)) != 0;
    }

    /**
     * Searches collection for doc containing "filterField": "filterValue"
     * Returns "field" element of first search result*/
    public static String getFieldValue(MongoCollection<Document> collection, String field, String filterField, String filterValue) throws JSONException {

        /*TODO: find more modern means of extracting field value from db search doc*/
        String json = collection.find(Filters.eq(filterField, filterValue)).first().toJson();
        JSONObject jsonObject = new JSONObject(json);
        try {
            return (String) jsonObject.get(field);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getFieldValue(Document doc, String field) {
        JSONObject jsonObject = new JSONObject(doc.toJson());
        try {
            return (String) jsonObject.get(field);
        } catch (JSONException e) {
            return null;
        }
    }
}
