package com.melnykov.fab.sample;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.UUID;

@ParseClassName("Debt")
public class Debt extends ParseObject {

    public String getTitle() {
        return getString("title");
    }

    public void setTitle(String title) {
        if (title != null) {
            put("title", title);
        } else {
            remove("title");
        }
    }

    public String getOwner() {
        return getString("owner");
    }

    public void setOwner(String owner) {
        if (owner != null) {
            put("owner", owner);
        } else {
            remove("owner");
        }
    }

    public String getDescription() {
        return getString("description");
    }

    public void setDescription(String description) {
        if (description != null) {
            put("description", description);
        } else {
            remove("description");
        }
    }

    public Date getDueDate() {
        return getDate("dueDate");
    }

    public void setDueDate(Date dueDate) {
        if (dueDate != null) {
            put("dueDate", dueDate);
        } else {
            remove("dueDate");
        }
    }

    public ParseUser getAuthor() {
        return getParseUser("author");
    }

    public void setAuthor(ParseUser currentUser) {
        if (currentUser != null) {
            put("author", currentUser);
        } else {
            remove("author");
        }
    }

    public boolean isDraft() {
        return getBoolean("isDraft");
    }

    public void setDraft(boolean isDraft) {
        put("isDraft", isDraft);
    }

    public void setUuidString() {
        UUID uuid = UUID.randomUUID();
        put("uuid", uuid.toString());
    }

    public String getUuidString() {
        return getString("uuid");
    }

    public static ParseQuery<Debt> getQuery() {
        return ParseQuery.getQuery(Debt.class);
    }
}
