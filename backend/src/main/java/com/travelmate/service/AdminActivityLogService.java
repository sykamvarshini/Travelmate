package com.travelmate.service;

import com.travelmate.entity.AdminActivityLog;
import com.travelmate.repository.AdminActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminActivityLogService {

    private final AdminActivityLogRepository logRepository;

    public AdminActivityLogService(AdminActivityLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional
    public AdminActivityLog logActivity(String adminEmail, String action, String details, String ipAddress) {
        AdminActivityLog log = new AdminActivityLog(adminEmail, action, details, ipAddress);
        return logRepository.save(log);
    }

    public List<AdminActivityLog> getRecentLogs() {
        return logRepository.findTop50ByOrderByTimestampDesc();
    }
}
