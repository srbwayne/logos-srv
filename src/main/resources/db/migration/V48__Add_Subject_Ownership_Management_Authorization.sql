ALTER TABLE authorization_grant
    DROP CONSTRAINT ck_authorization_grant_operation,
    DROP CONSTRAINT ck_authorization_grant_applicability;

ALTER TABLE authorization_grant
    ADD CONSTRAINT ck_authorization_grant_operation
        CHECK (operation IN (
            'PROGRESSION_EXECUTE',
            'PROGRESSION_EXECUTION_READ',
            'PROGRESSION_HISTORY_READ',
            'SUBJECT_PROVISION',
            'SUBJECT_OWNERSHIP_MANAGE'
        )),
    ADD CONSTRAINT ck_authorization_grant_applicability
        CHECK (
            (operation = 'PROGRESSION_EXECUTE' AND source IS NOT NULL AND namespace IS NOT NULL)
            OR (operation = 'PROGRESSION_EXECUTION_READ' AND source IS NOT NULL AND namespace IS NULL)
            OR (operation IN (
                    'PROGRESSION_HISTORY_READ',
                    'SUBJECT_PROVISION',
                    'SUBJECT_OWNERSHIP_MANAGE'
                ) AND source IS NULL AND namespace IS NOT NULL)
        );
