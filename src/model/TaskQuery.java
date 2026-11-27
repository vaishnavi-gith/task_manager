package model;
public final class TaskQuery {
    public final Boolean completed;
    public final Priority priority;
    public final String search;
    public final String sortBy;
    public final boolean descending;
    public final int page, pageSize;
    private TaskQuery(Builder b){completed=b.completed;priority=b.priority;search=b.search;sortBy=b.sortBy;descending=b.descending;page=b.page;pageSize=b.pageSize;}
    public static class Builder {
        private Boolean completed; private Priority priority; private String search,sortBy="id";
        private boolean descending; private int page=1,pageSize=10;
        public Builder completed(Boolean v){completed=v;return this;}
        public Builder priority(Priority v){priority=v;return this;}
        public Builder search(String v){search=v;return this;}
        public Builder sortBy(String v){sortBy=v;return this;}
        public Builder descending(boolean v){descending=v;return this;}
        public Builder page(int v){page=v;return this;}
        public Builder pageSize(int v){pageSize=v;return this;}
        public TaskQuery build(){return new TaskQuery(this);}
    }
}
