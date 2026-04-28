// Task Controller

package com.todo.todo_app.controller;

import com.todo.todo_app.model.Task;
import com.todo.todo_app.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // GET for tasks
    @GetMapping
    public List<Task> getTasks(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return taskService.getTasksForUser(userId);
    }

    // POST for tasks
    @PostMapping
    public Task createTask(@RequestBody Task task, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return taskService.createTask(task, userId);
    }

    // DELETE for tasks
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        taskService.deleteTask(id, userId);
    }

    // PUT for task toggle
    @PutMapping("/{id}/toggle")
    public Task toggleTask(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return taskService.toggleTask(id, userId);
    }

    // PUT for tasks
    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody Task taskDetails, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return taskService.updateTask(id, taskDetails, userId);
    }
}