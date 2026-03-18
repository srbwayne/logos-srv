package com.josecjuniors.logossrv.core.appuser.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "app_user")
public class AppUser extends AbstractDomainAggregate<AppUserId> implements UserDetails {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConexaoExterna> conexoes = new HashSet<>();

    // Construtor para JPA
    protected AppUser() {
        super();
    }

    public AppUser(AppUserId id, String email, String password) {
        super(id);
        this.email = email;
        this.password = password;
    }

    public void adicionarConexao(ConexaoExterna conexao) {
        this.conexoes.add(conexao);
    }

    public Optional<ConexaoExterna> getConexaoPorProvedor(ProvedorIntegracao provedor) {
        return conexoes.stream()
                .filter(c -> c.getProvedor() == provedor && c.isAtivo())
                .findFirst();
    }

    public Set<ConexaoExterna> getConexoes() {
        return Collections.unmodifiableSet(conexoes);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getEmail() {
        return email;
    }
}
