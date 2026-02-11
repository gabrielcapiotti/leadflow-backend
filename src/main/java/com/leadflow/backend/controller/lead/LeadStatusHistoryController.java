package com.leadflow.backend.controller.lead;

import com.leadflow.backend.dto.lead.LeadStatusHistoryResponse;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.lead.LeadStatusHistory;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.lead.LeadStatusHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadStatusHistoryController {

    private final LeadService leadService;
    private final LeadStatusHistoryService historyService;

    public LeadStatusHistoryController(
            LeadService leadService,
            LeadStatusHistoryService historyService
    ) {
        this.leadService = leadService;
        this.historyService = historyService;
    }

    /* ==========================
       HISTORY BY LEAD (ISOLADO)
       ========================== */

    @GetMapping("/{leadId}/history")
    public ResponseEntity<List<LeadStatusHistoryResponse>> getHistory(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long leadId
    ) {
        // Garante que o lead pertence ao usuário
        var lead = leadService.getByIdForUser(leadId, currentUser);

        List<LeadStatusHistoryResponse> response =
                historyService.getHistoryByLead(lead)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    /* ==========================
       MAPPER
       ========================== */

    private LeadStatusHistoryResponse toResponse(LeadStatusHistory history) {
        return new LeadStatusHistoryResponse(
                history.getId(),
                history.getStatus().name(),
                history.getUpdatedBy() != null
                        ? history.getUpdatedBy().getEmail()
                        : "SYSTEM",
                history.getChangedAt()
        );
    }
}
