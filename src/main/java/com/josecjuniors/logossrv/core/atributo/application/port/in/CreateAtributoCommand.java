package com.josecjuniors.logossrv.core.atributo.application.port.in;

/**
 * Comando para criar um novo Atributo.
 *
 * @param nome O nome do novo atributo (ex: 'Foco', 'Disciplina').
 */
public record CreateAtributoCommand(
        String nome
) {
    public CreateAtributoCommand {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do atributo não pode ser nulo.");
        }
    }
}
