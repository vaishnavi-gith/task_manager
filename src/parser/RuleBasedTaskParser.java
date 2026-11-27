package parser;
import java.time.LocalDate;
public class RuleBasedTaskParser implements TaskParser {
    public ParsedTask parse(String text,String today){
        if(text==null||text.isBlank())throw new IllegalArgumentException("Task text is empty");
        String title=text.trim(); String lower=title.toLowerCase(); String due=null;
        LocalDate date=LocalDate.parse(today);
        if(lower.contains("tomorrow")){due=date.plusDays(1).toString();title=title.replaceAll("(?i)\\btomorrow\\b","").trim();}
        String recurrence=lower.contains("every month")?"monthly":null;
        title=title.replaceAll("(?i)\\bevery month\\b","").trim();
        if(title.isBlank())title=text.trim();
        return new ParsedTask(title, "Created from: "+text, due, recurrence);
    }
}
