package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorAuditLog;
import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.security.VendorContext;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final VendorAuditLogRepository repository;
    private final VendorContext vendorContext;

    public AuditService(VendorAuditLogRepository repository,
                        VendorContext vendorContext) {
        this.repository = repository;
        this.vendorContext = vendorContext;
    }

    public void log(String acao,
                    UUID entidadeId,
                    String detalhes) {

        Vendor vendor = vendorContext.getCurrentVendor();

        VendorAuditLog log = new VendorAuditLog();
        log.setVendorId(vendor.getId());
        log.setUserEmail(vendor.getUserEmail());
        log.setAcao(acao);
        log.setEntidadeId(entidadeId);
        log.setDetalhes(detalhes);

        repository.save(log);
    }
}
