-- V20__Alter_Table_Valor_registrado_RegistroAtividadeDetalhe.sql



-- Adiciona as novas colunas para gerenciamento de processamento assincrono da atividade
ALTER TABLE registro_atividade_detalhe ALTER COLUMN valor_registrado TYPE varchar(100) USING valor_registrado::varchar(100);

