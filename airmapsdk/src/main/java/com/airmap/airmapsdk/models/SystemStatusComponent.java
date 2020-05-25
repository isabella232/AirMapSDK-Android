package com.airmap.airmapsdk.models;

public class SystemStatusComponent {

    public enum Level {
        DEGRADED, FAILED, MAINTENANCE, NORMAL, UNKNOWN
    }

    String id;
    String name;
    String level;
    String updated_at;
    String children;

    public SystemStatusComponent(String id, String name, String level, String updated_at, String children) {
        this.id = id;
        this.name = name;
        this.updated_at = updated_at;
        this.children = children;
        this.level = level;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getChildren() {
        return children;
    }

    public void setChildren(String children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "SystemStatusComponent{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", level=" + level +
                ", updated_at='" + updated_at + '\'' +
                ", children='" + children + '\'' +
                '}';
    }
}
