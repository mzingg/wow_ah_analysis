package mrwolf.dbimport.tasks;

import lombok.Setter;

import java.util.List;

public class TaskList {

  @Setter
  private List<Task> tasks;

  public void executeTasks() {
    for (Task task : tasks) {
      task.execute();
    }
  }

}
