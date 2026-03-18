package com.josecjuniors.logossrv.core.appuser.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "conexao_externa", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"app_user_id", "provedor"})
})
public class ConexaoExterna extends AbstractDomainAggregate<ConexaoExternaId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProvedorIntegracao provedor;

    @Column(name = "identificador_externo", nullable = false)
    private String identificadorExterno; // Ex: chat_id do Telegram, athlete_id do Strava

    @Column(name = "token_acesso", length = 1024)
    private String tokenAcesso; // Access Token

    @Column(name = "token_atualizacao", length = 1024)
    private String tokenAtualizacao; // Refresh Token

    @Column(name = "data_expiracao")
    private LocalDateTime dataExpiracao;

    @Column(nullable = false)
    private boolean ativo = true;

    protected ConexaoExterna() {
        super();
    }

    public ConexaoExterna(ConexaoExternaId id, AppUser appUser, ProvedorIntegracao provedor, String identificadorExterno) {
        super(id);
        this.appUser = appUser;
        this.provedor = provedor;
        this.identificadorExterno = identificadorExterno;
    }

    public void atualizarTokens(String tokenAcesso, String tokenAtualizacao, LocalDateTime dataExpiracao) {
        this.tokenAcesso = tokenAcesso;
        this.tokenAtualizacao = tokenAtualizacao;
        this.dataExpiracao = dataExpiracao;
    }
    
    public boolean isTokenExpirado() {
        return dataExpiracao != null && LocalDateTime.now().isAfter(dataExpiracao);
    }

    // Getters
    public AppUser getAppUser() { return appUser; }
    public ProvedorIntegracao getProvedor() { return provedor; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public String getTokenAcesso() { return tokenAcesso; }
    public String getTokenAtualizacao() { return tokenAtualizacao; }
    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public boolean isAtivo() { return ativo; }
    
    public void desativar() { this.ativo = false; }
    public void ativar() { this.ativo = true; }
}
