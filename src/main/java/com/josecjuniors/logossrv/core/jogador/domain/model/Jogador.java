package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.common.domain.Nivelavel;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorCommand;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.EnderecoEstado;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.Sexo;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.StatusPerfilJogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;

import java.time.LocalDate;
import java.time.Period;

@Entity
public class Jogador extends AbstractDomainAggregate<JogadorId> implements Nivelavel {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, unique = true)
    private String apelido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPerfilJogador statusPerfil;

    // Campos da interface Nivelavel
    @Column(nullable = false)
    private Integer nivelAtual;

    @Column(nullable = false)
    private Long xpTotal;

    // Campos do perfil
    private String nomeCompleto;
    private LocalDate dataNascimento;
    private String cpf;
    private String numeroTelefone;
    private Integer idade;
    @Enumerated(EnumType.STRING)
    private Sexo sexo;
    @Lob
    private String descricao;
    private String enderecoPais;
    @Enumerated(EnumType.STRING)
    private EnderecoEstado enderecoEstado;
    private String enderecoCidade;
    private String enderecoDescricao;
    private String enderecoComplemento;
    private String enderecoCep;

    protected Jogador() {
        super();
    }

    public Jogador(JogadorId id, AppUser user, String apelido) {
        super(id);
        this.user = user;
        this.apelido = apelido;
        this.statusPerfil = StatusPerfilJogador.INCOMPLETO;
        this.nivelAtual = 1;
        this.xpTotal = 0L;
    }

    public void atualizarPerfil(UpdatePerfilJogadorCommand command) {
        this.nomeCompleto = command.nomeCompleto();
        this.dataNascimento = command.dataNascimento();
        this.cpf = command.cpf();
        this.numeroTelefone = command.numeroTelefone();
        this.sexo = command.sexo();
        this.descricao = command.descricao();
        this.enderecoPais = command.enderecoPais();
        this.enderecoEstado = command.enderecoEstado();
        this.enderecoCidade = command.enderecoCidade();
        this.enderecoDescricao = command.enderecoDescricao();
        this.enderecoComplemento = command.enderecoComplemento();
        this.enderecoCep = command.enderecoCep();
        
        if (command.dataNascimento() != null) {
            this.idade = Period.between(command.dataNascimento(), LocalDate.now()).getYears();
        }

        this.statusPerfil = StatusPerfilJogador.COMPLETO;
    }

    public void atualizarApelido(String novoApelido) {
        if (novoApelido != null && !novoApelido.isBlank()) {
            this.apelido = novoApelido;
        }
    }

    @Override
    public void adicionarExperiencia(Long xpGanha) {
        this.xpTotal += xpGanha;
    }

    // Getters
    public AppUser getUser() { return user; }
    public String getApelido() { return apelido; }
    public StatusPerfilJogador getStatusPerfil() { return statusPerfil; }
    public String getNomeCompleto() { return nomeCompleto; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getCpf() { return cpf; }
    public String getNumeroTelefone() { return numeroTelefone; }
    public Integer getIdade() { return idade; }
    public Sexo getSexo() { return sexo; }
    public String getDescricao() { return descricao; }
    public String getEnderecoPais() { return enderecoPais; }
    public EnderecoEstado getEnderecoEstado() { return enderecoEstado; }
    public String getEnderecoCidade() { return enderecoCidade; }
    public String getEnderecoDescricao() { return enderecoDescricao; }
    public String getEnderecoComplemento() { return enderecoComplemento; }
    public String getEnderecoCep() { return enderecoCep; }

    // Getters da Interface Nivelavel
    @Override
    public Integer getNivelAtual() { return nivelAtual; }
    @Override
    public Long getXpTotal() { return xpTotal; }

    // Setters da Interface Nivelavel
    @Override
    public void setNivelAtual(Integer nivel) { this.nivelAtual = nivel; }
    @Override
    public void setXpTotal(Long xp) { this.xpTotal = xp; }
}
