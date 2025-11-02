package com.josecjuniors.logossrv.core.appuser.domain.repository;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;

import java.util.Optional;

public interface AppUserRepository {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    AppUser save(AppUser user);
}
