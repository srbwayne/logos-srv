package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.common.domain.Nivelavel;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorCommand;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.EnderecoEstado;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.Sexo;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.StatusPerfilJogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

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

    @OneToOne(mappedBy = "jogador", cascade = CascadeType.ALL, orphanRemoval = true)
    private EstresseGlobal estresseGlobal;

    @OneToMany(mappedBy = "jogador", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AtributoJogador> atributos = new HashSet<>();

    @OneToMany(mappedBy = "jogador", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HabilidadeJogador> habilidades = new HashSet<>();

    private Integer pontosHabilidade;


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
        this.pontosHabilidade = 1;
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

    public void aplicarEstresse(int valor) {
        this.estresseGlobal.adicionarEstresse(valor);
    }

    @Override
    public void adicionarExperiencia(Long xpGanha) {
        this.xpTotal += xpGanha;
    }

    // Getters
    public AppUser getUser() {
        return user;
    }

    public String getApelido() {
        return apelido;
    }

    public StatusPerfilJogador getStatusPerfil() {
        return statusPerfil;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getCpf() {
        return cpf;
    }

    public String getNumeroTelefone() {
        return numeroTelefone;
    }

    public Integer getIdade() {
        return idade;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getEnderecoPais() {
        return enderecoPais;
    }

    public EnderecoEstado getEnderecoEstado() {
        return enderecoEstado;
    }

    public String getEnderecoCidade() {
        return enderecoCidade;
    }

    public String getEnderecoDescricao() {
        return enderecoDescricao;
    }

    public String getEnderecoComplemento() {
        return enderecoComplemento;
    }

    public String getEnderecoCep() {
        return enderecoCep;
    }

    public Integer getPontosHabilidade() {
        return pontosHabilidade;
    }

    public EstresseGlobal getEstresseGlobal() {
        return estresseGlobal;
    }

    public void setEstresseGlobal(EstresseGlobal estresseGlobal) {
        this.estresseGlobal = estresseGlobal;
    }

    public Set<AtributoJogador> getAtributos() {
        return atributos;
    }

    public Set<HabilidadeJogador> getHabilidades() {
        return habilidades;
    }

    public AtributoJogador getAtributo(Atributo atributo) {
        return this.atributos.stream()
                .filter(a -> a.getAtributo().equals(atributo))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Jogador getJogadorAssociado() {
        return this;
    }

    // Getters da Interface Nivelavel
    @Override
    public Integer getNivelAtual() {
        return nivelAtual;
    }

    @Override
    public Long getXpTotal() {
        return xpTotal;
    }

    // Setters da Interface Nivelavel
    @Override
    public void setNivelAtual(Integer nivel) {
        this.nivelAtual = nivel;
    }

    @Override
    public void setXpTotal(Long xp) {
        this.xpTotal = xp;
    }

    public AtributoJogador adicionarAtributo(Atributo atributo) {
        // Verifica se já não possui para evitar duplicatas
        if (getAtributo(atributo) != null) {
            return getAtributo(atributo);
        }
        AtributoJogador novoAtributoJogador = new AtributoJogador(new AtributoJogadorId(), this, atributo);
        this.atributos.add(novoAtributoJogador);
        return novoAtributoJogador;
    }

    public void adicionarPontoDeHabilidade(){
        this.pontosHabilidade++;
    }
}
