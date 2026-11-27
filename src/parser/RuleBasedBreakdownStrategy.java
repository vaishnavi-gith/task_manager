package parser;
import java.util.*;
public class RuleBasedBreakdownStrategy implements BreakdownStrategy {
    public List<String[]> breakDown(String title,String description){
        List<String[]> out=new ArrayList<>();
        String[] parts=title.split("(?i)\\s+and\\s+|,");
        if(parts.length==1)parts=description.split("(?i)\\s+and\\s+|,");
        for(String p:parts){String s=p.trim();if(!s.isBlank())out.add(new String[]{s,"Subtask for: "+title});}
        if(out.isEmpty())out.add(new String[]{title+" - first step",description});
        return out;
    }
}
