package com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.api;

import com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.request.CreateProgressionConfigurationRequest;
import com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.request.UpdateProgressionConfigurationDraftRequest;
import com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.response.ProgressionConfigurationDraftResponse;
import com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.response.ProgressionConfigurationResponse;
import com.josecjuniors.logossrv.core.progressionconfiguration.application.service.ProgressionConfigurationAuthoringService;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDraft;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/progression/configurations")
public class ProgressionConfigurationAuthoringController {
    private final ProgressionConfigurationAuthoringService service;

    public ProgressionConfigurationAuthoringController(ProgressionConfigurationAuthoringService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProgressionConfigurationResponse> create(@RequestBody CreateProgressionConfigurationRequest request) {
        var response = ProgressionConfigurationResponse.from(service.create(request.logicalKey()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{logicalKey}").buildAndExpand(response.logicalKey()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{logicalKey}")
    public ResponseEntity<ProgressionConfigurationResponse> get(@PathVariable String logicalKey) {
        return ResponseEntity.ok(ProgressionConfigurationResponse.from(service.get(logicalKey)));
    }

    @GetMapping("/{logicalKey}/draft")
    public ResponseEntity<ProgressionConfigurationDraftResponse> getDraft(@PathVariable String logicalKey) {
        return ResponseEntity.ok(ProgressionConfigurationDraftResponse.from(service.getDraft(logicalKey)));
    }

    @PutMapping("/{logicalKey}/draft")
    public ResponseEntity<ProgressionConfigurationDraftResponse> replaceDraft(
            @PathVariable String logicalKey,
            @RequestBody UpdateProgressionConfigurationDraftRequest request) {
        var draft = new ProgressionConfigurationDraft(
                service.get(logicalKey).id(), request.expectedVersion(), request.baseXp(), request.baseStress(),
                request.factors() == null ? java.util.List.of() : request.factors(),
                request.distributions() == null ? java.util.List.of() : request.distributions().stream().map(d -> new ProgressionConfigurationDraft.Distribution(
                        d.attributeId(), d.weight(),
                        d.xpRules() == null ? java.util.List.of() : d.xpRules().stream().map(x -> new ProgressionConfigurationDraft.XpRule(x.fact(), x.multiplier(), x.minCutoff(), x.maxCutoff(), x.calculationMode())).toList(),
                        d.stressRules() == null ? java.util.List.of() : d.stressRules().stream().map(x -> new ProgressionConfigurationDraft.StressRule(x.multiplier(), x.minCutoff(), x.maxCutoff(), x.type())).toList())).toList());
        return ResponseEntity.ok(ProgressionConfigurationDraftResponse.from(service.replaceDraft(logicalKey, request.expectedVersion(), draft)));
    }
}
