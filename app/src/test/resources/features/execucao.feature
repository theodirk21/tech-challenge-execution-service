# language: pt
Funcionalidade: Execução da ordem de serviço
  O execution-service recebe eventos de outros serviços pelo SQS, expõe a API REST do mecânico
  e publica na outbox os eventos da saga.

  Cenário: OS paga entra na fila, é executada e finaliza
    Dado que existe a peça "Pastilha" com 5 unidades em estoque
    Quando o evento OS_CRIADA chega para a OS 10 com a solicitação "Barulho no freio" e o veículo "ABC1D23" modelo "Gol"
    Então a OS 10 fica "AGUARDANDO_DIAGNOSTICO"
    Quando o mecânico 7 conclui o diagnóstico da OS 10 com 2 unidades da peça "Pastilha" e o serviço 3
    Então a OS 10 fica "AGUARDANDO_PAGAMENTO"
    E o evento "DIAGNOSTICO_CONCLUIDO" é publicado para a OS 10
    Quando o evento PAGAMENTO_CONFIRMADO chega para a OS 10
    Então a OS 10 fica "NA_FILA"
    E o estoque da peça "Pastilha" é 3
    E o evento "ESTOQUE_RESERVADO" é publicado para a OS 10
    Quando o mecânico 7 inicia a próxima execução
    Então a OS 10 fica "EM_EXECUCAO"
    E o evento "EXECUCAO_INICIADA" é publicado para a OS 10
    Quando o mecânico 7 finaliza a execução da OS 10
    Então a OS 10 fica "FINALIZADA"
    E o evento "EXECUCAO_FINALIZADA" é publicado para a OS 10

  Cenário: Falta de estoque falha a execução sem reservar nada
    Dado que existe a peça "Disco" com 1 unidades em estoque
    Quando o evento OS_CRIADA chega para a OS 20 com a solicitação "Troca de disco" e o veículo "XYZ9K88" modelo "Uno"
    E o mecânico 7 conclui o diagnóstico da OS 20 com 9 unidades da peça "Disco" e o serviço 3
    E o evento PAGAMENTO_CONFIRMADO chega para a OS 20
    Então a OS 20 fica "FALHOU"
    E o estoque da peça "Disco" é 1
    E o evento "EXECUCAO_FALHOU" é publicado para a OS 20 com o motivo "SEM_ESTOQUE"

  Cenário: Cancelamento da OS compensa a reserva de peças
    Dado que existe a peça "Filtro" com 5 unidades em estoque
    Quando o evento OS_CRIADA chega para a OS 30 com a solicitação "Troca de filtro" e o veículo "CAN3C00" modelo "Palio"
    E o mecânico 7 conclui o diagnóstico da OS 30 com 2 unidades da peça "Filtro" e o serviço 3
    E o evento PAGAMENTO_CONFIRMADO chega para a OS 30
    Então a OS 30 fica "NA_FILA"
    E o estoque da peça "Filtro" é 3
    Quando o evento OS_CANCELADA chega para a OS 30
    Então a OS 30 fica "CANCELADA"
    E o estoque da peça "Filtro" é 5