package com.melnykov.fab.sample;

import android.annotation.TargetApi;
import android.os.Build;
import android.telephony.PhoneNumberUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@ParseClassName("Debt")
public class Debt extends ParseObject implements Serializable {

    public static final String KEY_UUID = "uuid";
    public static final String KEY_IS_DRAFT = "isDraft";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_AUTHOR_NAME = "authorName";
    public static final String KEY_AUTHOR_PHONE = "authorPhone";
    public static final String KEY_DUE_DATE = "dueDate";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_TITLE = "title";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_PHONE = "phone";
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
            put(KEY_TITLE, title.trim());
        } else {
            remove(KEY_TITLE);
        }
    }

    public String getOwner() {
        return getString(KEY_OWNER);
    }

    public void setOwner(String owner) {
        if (owner != null) {
            put(KEY_OWNER, owner.trim());
        } else {
            remove(KEY_OWNER);
        }
    }

    public String getPhone() {
        return getString(KEY_PHONE);
    }

    public void setPhone(String phone, String userCountry) {
        if (phone != null) {
            // Format phone number to E164 standard to use it as a unique identifier
            put(KEY_PHONE, formatToE164(phone, userCountry));
        } else {
            remove(KEY_PHONE);
        }
    }

    private String formatToE164(String phone, String userCountry) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber numberProto = null;
        try {
            numberProto = phoneUtil.parse(phone, userCountry);
        } catch (NumberParseException e) {
            System.err.println("NumberParseException was thrown: " + e.toString());
        }
        String formatted = null;
        if (numberProto != null) {
            formatted = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
        }
        return (formatted != null ? formatted : phone.replaceAll("[^0-9+]+", ""));
    }


    public String getDescription() {
        return getString(KEY_DESCRIPTION);
    }

    public void setDescription(String description) {
        if (description != null) {
            put(KEY_DESCRIPTION, description.trim());
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

    @Deprecated
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
    public String getAuthorName() {
        return getString(KEY_AUTHOR_NAME);
    }

    public void setAuthorName(String authorName) {
        if (authorName != null) {
            put(KEY_AUTHOR_NAME, authorName);
        } else {
            remove(KEY_AUTHOR_NAME);
        }
    }
    public String getAuthorPhone() {
        return getString(KEY_AUTHOR_PHONE);
    }

    public void setAuthorPhone(String authorPhone) {
        if (authorPhone != null) {
            put(KEY_AUTHOR_PHONE, authorPhone);
        } else {
            remove(KEY_AUTHOR_PHONE);
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
