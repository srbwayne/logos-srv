package com.josecjuniors.logossrv.core.progression.authoring;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service.*;
import com.josecjuniors.logossrv.core.regrafatorxp.application.service.*;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
class LegacyProgressionTombstoneServiceTest {
 @Test void distributionServicesRejectWithoutPersistenceAccess() {
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new CreateRegraDistribuicaoService().create(null));
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new UpdateRegraDistribuicaoService().update(null));
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new DeleteRegraDistribuicaoService().delete(null));
 }
 @Test void xpServicesRejectWithoutPersistenceAccess() {
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new CreateRegraFatorXPService().create(null));
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new UpdateRegraFatorXPService().update(null));
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new DeleteRegraFatorXPService().delete(null));
 }
 @Test void stressServicesRejectWithoutPersistenceAccess() {
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new CreateRegraFatorEstresseService().create(null));
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new UpdateRegraFatorEstresseService().update(null));
  assertThrows(LegacyProgressionAuthoringRetiredException.class, () -> new DeleteRegraFatorEstresseService().delete(null));
 }
}
