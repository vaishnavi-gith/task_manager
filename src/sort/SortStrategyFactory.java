package sort;
public final class SortStrategyFactory {
    private SortStrategyFactory(){}
    public static SortStrategy create(String key){
        if(key==null)return new IdSortStrategy();
        switch(key.toLowerCase()){case "title":return new TitleSortStrategy();case "priority":return new PrioritySortStrategy();case "duedate":return new DueDateSortStrategy();default:return new IdSortStrategy();}
    }
}
