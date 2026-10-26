ALTER TABLE tasks ADD COLUMN depends_on_task_id BIGINT NULL;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_depends_on FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE SET NULL;
CREATE INDEX idx_tasks_depends_on ON tasks(depends_on_task_id);
