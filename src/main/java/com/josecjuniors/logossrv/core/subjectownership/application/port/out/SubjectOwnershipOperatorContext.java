package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

/** Resolves a trusted operator authorized to manage ownership in one namespace. */
public interface SubjectOwnershipOperatorContext {

    AuthorizedSubjectOwnershipOperator authorizeForNamespace(String namespace);
}
