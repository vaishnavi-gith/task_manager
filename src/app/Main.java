package app;
import api.*;
import model.*;
import event.*;
import java.util.*;
public class Main {
    public static void main(String[] args) {
        TaskApi api=new TaskApi();
        ConsoleLogListener console=new ConsoleLogListener();
        AuditTrailListener audit=new AuditTrailListener();
        api.getService().addListener(console);api.getService().addListener(audit);

        System.out.println("--- CREATE TASK ---");
        System.out.println(api.createTask(new TaskRequest("Prepare project submission","Finish code and documentation","false","2026-12-01","HIGH")));
        System.out.println("\\n--- CREATE SUBTASK ---");
        System.out.println(api.createSubtask(1,new TaskRequest("Finish Java classes","Compile and test all classes","false",null,"MEDIUM")));
        System.out.println("\\n--- LIST SUBTASKS ---");
        System.out.println(api.listSubtasks(1));
        System.out.println("\\n--- FILTER / SEARCH / SORT / PAGE ---");
        System.out.println(api.query(new TaskQuery.Builder().completed(false).search("Java").sortBy("title").page(1).pageSize(5).build()));
        System.out.println("\\n--- AI-NATIVE CREATION (OpenAI when LLM_API_KEY is set; offline rules otherwise) ---");
        System.out.println(api.createTaskFromText("Pay rent tomorrow"));
        System.out.println("\\n--- VALIDATION ERROR ---");
        System.out.println(api.createTask(new TaskRequest(" ","","maybe","2026-02-30","URGENT")));
        System.out.println("\\n--- UNKNOWN ID ---");
        System.out.println(api.getTask(999));
        System.out.println("\\n--- BREAKDOWN ---");
        System.out.println(api.breakDownTask(1));
        System.out.println("\\n--- DELETE PARENT (ALSO DELETES SUBTASKS) ---");
        System.out.println(api.deleteTask(1));
        System.out.println(api.getAllTasks());
        api.getService().removeListener(console);
        System.out.println("\\n--- LISTENER REMOVAL CHECK ---");
        System.out.println(api.createTask(new TaskRequest("Listener check","Console listener was removed","false",null,"LOW")));
        System.out.println("Audit event counts: "+audit.getCounts());
    }
}
