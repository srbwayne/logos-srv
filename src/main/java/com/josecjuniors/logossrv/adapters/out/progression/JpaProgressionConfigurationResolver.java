package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaProgressionConfigurationResolver implements ProgressionConfigurationResolver {

    private final AtividadeConfigRepository atividadeConfigRepository;
    private final HabilidadeRepository habilidadeRepository;

    public JpaProgressionConfigurationResolver(AtividadeConfigRepository atividadeConfigRepository,
                                               HabilidadeRepository habilidadeRepository) {
        this.atividadeConfigRepository = atividadeConfigRepository;
        this.habilidadeRepository = habilidadeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProgressionConfiguration> resolve(ProgressionConfigurationReference reference) {
        return atividadeConfigRepository.findById(new AtividadeConfigId(reference.value()))
                .map(this::toConfiguration);
    }

    private ProgressionConfiguration toConfiguration(AtividadeConfig config) {
        var distributions = config.getRegrasDistribuicao().stream()
                .map(this::toDistribution)
                .toList();
        var skillRules = habilidadeRepository.findAll().stream()
                .flatMap(skill -> skill.getRegrasDistribuicao().stream()
                        .map(rule -> new ProgressionConfiguration.SkillBonusRule(
                                skill.getId().getValue().toString(),
                                rule.getAtributo().getId().getValue().toString(),
                                rule.getPesoDistribuicao())))
                .toList();
        return new ProgressionConfiguration(config.getXpBase(), config.getEstresseBase(), distributions, skillRules);
    }

    private ProgressionConfiguration.AttributeDistribution toDistribution(RegraDistribuicaoAtividade distribution) {
        return new ProgressionConfiguration.AttributeDistribution(
                distribution.getAtributo().getId().getValue().toString(),
                distribution.getPesoPercentual(),
                distribution.getRegraFatorXPS().stream().map(this::toXpRule).toList(),
                distribution.getRegraFatorEstresses().stream().map(this::toStressRule).toList());
    }

    private ProgressionConfiguration.XpRule toXpRule(RegraFatorXP rule) {
        return new ProgressionConfiguration.XpRule(
                rule.getFatorCalculo().getId().getValue().toString(), rule.getPesoMultiplicador(),
                rule.getPontoCorteMin(), rule.getPontoCorteMax());
    }

    private ProgressionConfiguration.StressRule toStressRule(RegraFatorEstresse rule) {
        return new ProgressionConfiguration.StressRule(
                rule.getPesoMultiplicador(), rule.getPontoCorteMin(), rule.getPontoCorteMax(),
                rule.getTipo() == com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse.NEGATIVO
                        ? ProgressionConfiguration.StressType.NEGATIVE : ProgressionConfiguration.StressType.POSITIVE);
    }
}
