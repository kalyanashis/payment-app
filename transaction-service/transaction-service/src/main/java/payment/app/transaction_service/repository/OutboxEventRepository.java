package payment.app.transaction_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import payment.app.transaction_service.model.dto.OutboxStatusType;
import payment.app.transaction_service.model.entity.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatusType status);
}
