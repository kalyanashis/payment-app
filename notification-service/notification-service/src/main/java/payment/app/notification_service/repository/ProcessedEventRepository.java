package payment.app.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import payment.app.notification_service.model.ProcessedEvent_old;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent_old, Long> {

    boolean existsByEventId(String eventId);
}
