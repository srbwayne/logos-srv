package com.josecjuniors.logossrv.adapters.out.appuser.jpa;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.appuser.domain.repository.AppUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserJpaRepository extends AppUserRepository, JpaRepository<AppUser, AppUserId> {

    @Override
    Optional<AppUser> findByEmail(String email);

    @Override
    boolean existsByEmail(String email);

    @Override
    AppUser save(AppUser user);
}
