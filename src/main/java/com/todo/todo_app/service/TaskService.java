// Task Service

package com.todo.todo_app.service;

import com.todo.todo_app.model.Task;
import com.todo.todo_app.model.User;
import com.todo.todo_app.repository.TaskRepository;
import com.todo.todo_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    // Fetch tasks for user
    @Cacheable(value = "tasks", key = "#userId")
    public List<Task> getTasksForUser(Long userId) {
        return taskRepository.findByUserUserId(userId);
    }

    // Create a task
    @CacheEvict(value = "tasks", key = "#userId")
    public Task createTask(Task task, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isPremium()) {
            long taskCount = taskRepository.countByUserUserId(userId);
            if (taskCount >= 5) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Free tier limit reached. Upgrade to Premium for unlimited tasks.");
            }
        }

        task.setUser(user);
        return taskRepository.save(task);
    }

    // Delete task
    @Transactional
    @CacheEvict(value = "tasks", key = "#userId")
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        verifyOwnership(task, userId);
        taskRepository.delete(task);
    }

    // Toggle status
    @Transactional
    @CacheEvict(value = "tasks", key = "#userId")
    public Task toggleTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        verifyOwnership(task, userId);
        task.setStatus(!task.isStatus());
        return taskRepository.save(task);
    }

    // Edit task
    @Transactional
    @CacheEvict(value = "tasks", key = "#userId")
    public Task updateTask(Long id, Task taskDetails, Long userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        verifyOwnership(task, userId);

        if (taskDetails.getTitle() != null && !taskDetails.getTitle().isEmpty()) {
            task.setTitle(taskDetails.getTitle());
        }
        task.setStatus(taskDetails.isStatus());
        if (taskDetails.getDueDateTime() != null) {
            task.setDueDateTime(taskDetails.getDueDateTime());
        } else {
            task.setDueDateTime(null);
        }

        return taskRepository.save(task);
    }

    private void verifyOwnership(Task task, Long userId) {
        if (task.getUser() == null || !task.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied. You cannot modify a task that doesn't belong to you.");
        }
    }
}