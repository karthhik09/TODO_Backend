// Task Repository

package com.todo.todo_app.repository;

import com.todo.todo_app.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserUserId(Long userId);

    long countByUserUserId(Long userId);
}