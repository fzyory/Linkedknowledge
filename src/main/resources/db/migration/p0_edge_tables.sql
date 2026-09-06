CREATE TABLE IF NOT EXISTS node_edge (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    edge_type VARCHAR(32) NOT NULL,
    edge_status VARCHAR(32) NOT NULL,
    source_text VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6),
    rejected_at DATETIME(6),
    INDEX idx_source (source_node_id),
    INDEX idx_target (target_node_id),
    INDEX idx_status_type (edge_status, edge_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_edge_suggestion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    score DOUBLE NOT NULL,
    suggestion_reason VARCHAR(500),
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6),
    INDEX idx_user_status (user_id, status),
    INDEX idx_source (source_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
