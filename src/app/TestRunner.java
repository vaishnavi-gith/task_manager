package app;
import api.*;
import model.*;
import repository.*;
import service.*;
import parser.*;
import exception.*;
import event.*;
import java.util.*;

public class TestRunner {
    private static int passed=0,failed=0;
    private static void check(String name,boolean ok){if(ok){passed++;System.out.println("PASS: "+name);}else{failed++;System.out.println("FAIL: "+name);}}
    public static void main(String[] args){
        InMemoryTaskRepository repo=InMemoryTaskRepository.getInstance(); repo.clear(); IdempotencyStore.getInstance().clear();
        TaskApi api=new TaskApi(new TaskService(repo));
        check("create returns 201",api.createTask(new TaskRequest("Test task","Description","false",null,"LOW")).getStatusCode()==201);
        check("get existing task returns 200",api.getTask(1).getStatusCode()==200);
        check("unknown task returns 404",api.getTask(999).getStatusCode()==404);
        check("blank fields and invalid values return 400",api.createTask(new TaskRequest(" ","","yes","2026-02-30","URGENT")).getStatusCode()==400);
        check("valid subtask returns 201",api.createSubtask(1,new TaskRequest("Child","Child description","false",null,"MEDIUM")).getStatusCode()==201);
        check("subtasks are listed",api.listSubtasks(1).getBody().contains("Child"));
        check("subtask cannot have subtasks",api.createSubtask(2,new TaskRequest("Grandchild","No","false",null,"LOW")).getStatusCode()==400);
        check("filter query works",api.query(new TaskQuery.Builder().priority(Priority.LOW).build()).getBody().contains("Test task"));
        check("search, sort and pagination combine",api.query(new TaskQuery.Builder().search("task").sortBy("title").page(1).pageSize(1).build()).getStatusCode()==200);
        check("page beyond final page returns empty list",api.query(new TaskQuery.Builder().page(99).pageSize(2).build()).getBody().equals("[]"));
        check("delete parent works",api.deleteTask(1).getStatusCode()==200);
        check("deleting parent deletes child",api.getTask(2).getStatusCode()==404);
        ApiResponse idemA=api.createTask(new TaskRequest("Idempotent","same request","false",null,"MEDIUM"),"idem-1");
        ApiResponse idemB=api.createTask(new TaskRequest("Idempotent","same request","false",null,"MEDIUM"),"idem-1");
        check("same idempotency key returns original response",idemA.getBody().equals(idemB.getBody()));
        check("same key with different request returns 422",api.createTask(new TaskRequest("Different","different","false",null,"LOW"),"idem-1").getStatusCode()==422);
        final List<String> observed=new ArrayList<>();
        TaskEventListener listener=(type,task)->observed.add(type);
        api.getService().addListener(listener);
        api.updateTask(3,new TaskRequest("Idempotent","same request","true",null,"MEDIUM"));
        check("completion update emits COMPLETED event",observed.contains("COMPLETED"));
        api.getService().removeListener(listener);

        repo.clear(); IdempotencyStore.getInstance().clear();
        FakeLlmClient good=new FakeLlmClient("{\"title\":\"Pay rent\",\"description\":\"Monthly rent payment\",\"dueDate\":\"2026-12-01\",\"recurrence\":\"monthly\"}");
        TaskService llmService=new TaskService(repo,new LlmTaskParser(good,false),new RuleBasedBreakdownStrategy());
        TaskApi llmApi=new TaskApi(llmService);
        ApiResponse ai1=llmApi.createTaskFromText("Remind me to pay rent on December 1", "llm-key");
        ApiResponse ai2=llmApi.createTaskFromText("Remind me to pay rent on December 1", "llm-key");
        check("fake LLM creates structured task",ai1.getStatusCode()==201&&ai1.getBody().contains("Pay rent"));
        check("idempotent AI retry does not call fake twice",good.getCallCount()==1&&ai1.getBody().equals(ai2.getBody()));

        repo.clear(); IdempotencyStore.getInstance().clear();
        FakeLlmClient bad=new FakeLlmClient("{\"title\":\"\",\"description\":\"bad output\",\"dueDate\":\"2026-02-30\"}");
        TaskApi badApi=new TaskApi(new TaskService(repo,new LlmTaskParser(bad,false),new RuleBasedBreakdownStrategy()));
        check("bad LLM output returns 502",badApi.createTaskFromText("bad task").getStatusCode()==502);
        check("bad LLM output saves no task",repo.findAll().isEmpty());
        FakeLlmClient impossibleDate=new FakeLlmClient("{\"title\":\"Pay rent\",\"description\":\"Monthly payment\",\"dueDate\":\"2026-02-30\"}");
        TaskApi impossibleDateApi=new TaskApi(new TaskService(repo,new LlmTaskParser(impossibleDate,false),new RuleBasedBreakdownStrategy()));
        check("impossible date from LLM returns 502",impossibleDateApi.createTaskFromText("pay rent").getStatusCode()==502);
        check("invalid LLM task is not saved",repo.findAll().isEmpty());
        TaskApi throwingApi=new TaskApi(new TaskService(repo,new LlmTaskParser(new FaultyLlmClient(null,true),false),new RuleBasedBreakdownStrategy()));
        check("LLM exception returns 502",throwingApi.createTaskFromText("network failure").getStatusCode()==502);

        repo.clear();
        TaskApi parentApi=new TaskApi(new TaskService(repo));
        parentApi.createTask(new TaskRequest("Big task","Large project","false",null,"HIGH"));
        TaskApi breakdownApi=new TaskApi(new TaskService(repo,new RuleBasedTaskParser(),new LlmBreakdownStrategy(new FakeLlmClient("Step one|Do first thing\nStep two|Do second thing"),false)));
        check("LLM breakdown creates all valid subtasks",breakdownApi.breakDownTask(1).getStatusCode()==201&&repo.findAll().size()==3);
        repo.clear();
        TaskApi parentForBadBreakdown=new TaskApi(new TaskService(repo));
        parentForBadBreakdown.createTask(new TaskRequest("Atomic task","Must not partially save","false",null,"HIGH"));
        TaskApi invalidBreakdownApi=new TaskApi(new TaskService(repo,new RuleBasedTaskParser(),new LlmBreakdownStrategy(new FakeLlmClient("Valid one|This is okay\nInvalid item|"),false)));
        check("invalid breakdown returns 502",invalidBreakdownApi.breakDownTask(1).getStatusCode()==502);
        check("invalid breakdown saves no partial subtasks",repo.findAll().size()==1);
        repo.clear(); IdempotencyStore.getInstance().clear();
        TaskApi parentForRetry=new TaskApi(new TaskService(repo));
        parentForRetry.createTask(new TaskRequest("Retry breakdown","Task details","false",null,"HIGH"));
        FakeLlmClient breakdownFake=new FakeLlmClient("One|First action\nTwo|Second action");
        TaskApi retryBreakdownApi=new TaskApi(new TaskService(repo,new RuleBasedTaskParser(),new LlmBreakdownStrategy(breakdownFake,false)));
        ApiResponse breakdownFirst=retryBreakdownApi.breakDownTask(1,"breakdown-retry");
        ApiResponse breakdownSecond=retryBreakdownApi.breakDownTask(1,"breakdown-retry");
        check("breakdown retry does not call fake twice",breakdownFake.getCallCount()==1&&breakdownFirst.getBody().equals(breakdownSecond.getBody()));

        System.out.println("\nPassed: "+passed+" Failed: "+failed);
        if(failed>0)System.exit(1);
    }
}
