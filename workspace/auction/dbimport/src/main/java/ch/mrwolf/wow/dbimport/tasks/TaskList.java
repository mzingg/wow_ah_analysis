package ch.mrwolf.wow.dbimport.tasks;

import java.util.List;

import lombok.Setter;

public class TaskList {

  @Setter
  private List<Task> tasks;

  public void executeTasks() {
    for (Task task : tasks) {
      task.execute();
    }
  }

}
