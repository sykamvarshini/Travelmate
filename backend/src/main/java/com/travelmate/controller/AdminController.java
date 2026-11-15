package com.travelmate.controller;

import com.travelmate.dto.AdminOverviewResponse;
import com.travelmate.entity.AdminActivityLog;
import com.travelmate.service.AdminActivityLogService;
import com.travelmate.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminActivityLogService activityLogService;

    public AdminController(AdminService adminService, AdminActivityLogService activityLogService) {
        this.adminService = adminService;
        this.activityLogService = activityLogService;
    }

    /**
     * Overview statistics, recent registered users, recent trips, and recent activity logs.
     * All metrics are computed dynamically from the live MySQL database.
     */
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> getOverview() {
        AdminOverviewResponse overview = adminService.getOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * Retrieve the latest administrative activity logs.
     */
    @GetMapping("/activity-logs")
    public ResponseEntity<List<AdminActivityLog>> getActivityLogs() {
        List<AdminActivityLog> logs = activityLogService.getRecentLogs();
        return ResponseEntity.ok(logs);
    }

    /**
     * Record an administrative action into the activity log (e.g. system refresh or destination updates).
     */
    @PostMapping("/log-action")
    public ResponseEntity<AdminActivityLog> logAction(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = (auth != null && auth.isAuthenticated()) ? auth.getName() : "Administrator";
        String action = payload.getOrDefault("action", "ADMIN_ACTION");
        String details = payload.getOrDefault("details", "Administrative action performed");
        String clientIp = getClientIp(request);

        AdminActivityLog log = activityLogService.logActivity(adminEmail, action, details, clientIp);
        return ResponseEntity.ok(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
