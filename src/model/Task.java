package model;

public final class Task {
    private final int id;
    private final String title, description, dueDate;
    private final boolean completed;
    private final Priority priority;
    private final Integer parentId;

    private Task(Builder b) {
        this.id=b.id; this.title=b.title; this.description=b.description;
        this.completed=b.completed; this.dueDate=b.dueDate;
        this.priority=b.priority; this.parentId=b.parentId;
    }
    public int getId(){return id;}
    public String getTitle(){return title;}
    public String getDescription(){return description;}
    public boolean isCompleted(){return completed;}
    public String getDueDate(){return dueDate;}
    public Priority getPriority(){return priority;}
    public Integer getParentId(){return parentId;}
    public Builder toBuilder(){return new Builder(title,description,completed).id(id).dueDate(dueDate).priority(priority).parentId(parentId);}
    public static class Builder {
        private int id;
        private final String title, description;
        private final boolean completed;
        private String dueDate;
        private Priority priority=Priority.MEDIUM;
        private Integer parentId;
        public Builder(String title,String description,boolean completed){this.title=title;this.description=description;this.completed=completed;}
        public Builder id(int v){id=v;return this;}
        public Builder dueDate(String v){dueDate=v;return this;}
        public Builder priority(Priority v){priority=v==null?Priority.MEDIUM:v;return this;}
        public Builder parentId(Integer v){parentId=v;return this;}
        public Task build(){return new Task(this);}
    }
}
