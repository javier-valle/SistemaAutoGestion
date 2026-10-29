-- Coherencia: no se puede afirmar que un nombre vino de una fuente externa si
-- no se guardó cuál fue. Los registros anteriores a V7 (incluidos los datos
-- demo, escritos por el personal) quedan como datos declarados.
UPDATE cliente
SET documento_verificado = FALSE
WHERE fuente_verificacion IS NULL;
