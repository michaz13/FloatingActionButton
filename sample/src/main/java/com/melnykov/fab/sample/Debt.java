package com.melnykov.fab.sample;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.UUID;

@ParseClassName("Debt")
public class Debt extends ParseObject {

    public static final String KEY_UUID = "uuid";
    public static final String KEY_IS_DRAFT = "isDraft";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_DUE_DATE = "dueDate";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_TITLE = "title";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_TAB_TAG = "tabTag";
    public static final String I_OWE_TAG = "iOwe";
    public static final String OWE_ME_TAG = "oweMe";

    public String getTabTag() {
        return getString(KEY_TAB_TAG);
    }

    public void setTabTag(String tabTag) {
        if (tabTag != null) {
            put(KEY_TAB_TAG, tabTag);
        } else {
            remove(KEY_TAB_TAG);
        }
    }
    public String getTitle() {
        return getString(KEY_TITLE);
    }

    public void setTitle(String title) {
        if (title != null) {
            put(KEY_TITLE, title);
        } else {
            remove(KEY_TITLE);
        }
    }

    public String getOwner() {
        return getString(KEY_OWNER);
    }

    public void setOwner(String owner) {
        if (owner != null) {
            put(KEY_OWNER, owner);
        } else {
            remove(KEY_OWNER);
        }
    }

    public String getDescription() {
        return getString(KEY_DESCRIPTION);
    }

    public void setDescription(String description) {
        if (description != null) {
            put(KEY_DESCRIPTION, description);
        } else {
            remove(KEY_DESCRIPTION);
        }
    }

    public Date getDueDate() {
        return getDate(KEY_DUE_DATE);
    }

    public void setDueDate(Date dueDate) {
        if (dueDate != null) {
            put(KEY_DUE_DATE, dueDate);
        } else {
            remove(KEY_DUE_DATE);
        }
    }

    public ParseUser getAuthor() {
        return getParseUser(KEY_AUTHOR);
    }

    public void setAuthor(ParseUser currentUser) {
        if (currentUser != null) {
            put(KEY_AUTHOR, currentUser);
        } else {
            remove(KEY_AUTHOR);
        }
    }

    public boolean isDraft() {
        return getBoolean(KEY_IS_DRAFT);
    }

    public void setDraft(boolean isDraft) {
        put(KEY_IS_DRAFT, isDraft);
    }

    public void setUuidString() {
        UUID uuid = UUID.randomUUID();
        put(KEY_UUID, uuid.toString());
    }

    public String getUuidString() {
        return getString(KEY_UUID);
    }

    public static ParseQuery<Debt> getQuery() {
        return ParseQuery.getQuery(Debt.class);
    }
}
