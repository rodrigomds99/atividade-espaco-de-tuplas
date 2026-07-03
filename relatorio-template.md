# Relatório — Atividade Prática: Espaço de Tuplas com Apache River

**Disciplina:*INE5418-05208 - Computação Distribuída*
**Dupla:*Rodrigo Martins dos Santos* / Leonardo Fonseca Franchini
**Data:*27/06/2025*

---

## Nível 0 — Rodar e observar

### Observação inicial

1. Qual serviço aparece nos logs primeiro? Por que ele precisa existir antes dos outros?

> _Resposta:_ O reggie aparece primeiro, pois ele é responsável pelo serviço de descoberta. Os demais componentes precisam dele para localizar o JavaSpace.

2. O produtor menciona o nome ou o endereço do consumidor em algum momento? O consumidor menciona o produtor?

> _Resposta:_ Não. Ambos apenas localizam o espaço de tuplas e se comunicam por meio dele.

3. O consumidor começa a processar tarefas antes que o produtor termine de depositar todas? O que isso diz sobre como os dois se coordenam?

> _Resposta:_ Sim. Isso mostra que produtor e consumidor trabalham de forma assíncrona e são coordenados pelo espaço de tuplas.

---

### Experimento de desacoplamento temporal

4. O consumidor encontrou as tarefas mesmo sendo iniciado depois que o produtor já havia encerrado? O que isso demonstra?

> _Resposta:_ Sim. Isso demonstra o desacoplamento temporal, pois as tarefas permanecem armazenadas no espaço até serem consumidas.

5. Em comunicação direta via socket, seria possível esse comportamento? Por quê não?

> _Resposta:_ Não. Em uma comunicação direta os dois processos normalmente precisam estar ativos ao mesmo tempo para trocar mensagens.

---

## Nível 1 — Inspecionar

### 1.1 As três operações

Preencha a tabela com base no que você observou nos logs:

| Operação River | Equivalente Linda | O que ela faz? | Bloqueia quando não encontra correspondência? | Altera o estado do espaço? |
|---------------|-------------------|----------------|----------------------------------------------|---------------------------|
| `write(entry)`   | `OUT` | Insere uma tupla no espaço. | Não. | Sim. |
| `take(template)` | `IN`  | Remove e retorna uma tupla compatível. | Sim. | Sim. |
| `read(template)` | `RD`  | Lê uma tupla sem removê-la.| Sim. | Não.|

---

### 1.2 O papel do `reggie`

1. Quando o `reggie` caiu, os serviços que já estavam conectados ao espaço continuaram funcionando? Por quê?

> _Resposta:_ Sim. Após descobrir o serviço, a comunicação ocorre diretamente com o espaço de tuplas.

2. O que aconteceria com um produtor ou consumidor que tentasse iniciar enquanto o `reggie` estivesse fora do ar?

> _Resposta:_ Eles não conseguiriam localizar o espaço de tuplas e não iniciariam corretamente.

3. Qual sistema moderno cumpre papel equivalente ao `reggie` em uma arquitetura de microsserviços?

> _Resposta:_ Um serviço de descoberta, como Consul, Eureka ou Kubernetes Service Discovery.

---

### 1.3 Desacoplamento espacial

1. O produtor tem qualquer informação sobre quantos consumidores existem?

> _Resposta:_ Não.

2. O consumidor tem qualquer informação sobre quem produziu a tarefa que ele retirou?

> _Resposta:_ Não.

3. Como produtor e consumidor se coordenam se não se conhecem?

> _Resposta:_ Ambos utilizam o espaço de tuplas como intermediário para compartilhar as tarefas.

---

### 1.4 Comportamento de bloqueio

1. O que o consumidor fez enquanto o espaço estava vazio?

> _Observado:_ Ficou aguardando até aparecer uma tarefa compatível.

2. Quando o produtor depositou a primeira tarefa, o que aconteceu imediatamente?

> _Observado:_ O consumidor retirou a tarefa e iniciou seu processamento.

3. Esse comportamento tem nome no modelo Linda. Qual é e por que ele é útil em sistemas distribuídos reais?

> _Resposta:_ É o bloqueio da operação IN (take). Ele evita espera ativa e sincroniza automaticamente produtor e consumidor.

---

### 1.5 Escalabilidade horizontal

1. Uma mesma tarefa foi processada por dois consumidores ao mesmo tempo?

> _Observado:_ Não.

2. O produtor precisou ser modificado para suportar dois consumidores?

> _Resposta:_ Não.

3. Esse comportamento tem um nome em arquitetura de sistemas. Qual é?

> _Resposta:_ Balanceamento de carga (Load Balancing).

---

## Nível 2 — Modificar

### 2.1 Modificação A — Prioridade de tarefas

1. As tarefas de prioridade alta foram processadas antes das de prioridade baixa? Cole um trecho dos logs que evidencie isso:

> _Resposta:_ Sim. O consumidor sempre buscou primeiro tarefas de prioridade alta (prioridade = 1). Somente após esgotá-las passou a consumir as tarefas de prioridade baixa (prioridade = 2), conforme demonstrado pelos logs abaixo.

```
[PRODUTOR] Espaço encontrado via lookup.
[CONSUMIDOR-1] Espaço encontrado. Aguardando tarefas...

[PRODUTOR] write: TaskEntry{id=1, tipo="calcular", prioridade=2}
[PRODUTOR] write: TaskEntry{id=2, tipo="calcular", prioridade=1}
[CONSUMIDOR-1] take: TaskEntry{id=2, tipo="calcular", prioridade=1}

[PRODUTOR] write: TaskEntry{id=3, tipo="calcular", prioridade=2}
[CONSUMIDOR-1] Processamento concluído: tarefa 2

[PRODUTOR] write: TaskEntry{id=4, tipo="calcular", prioridade=1}
[CONSUMIDOR-1] take: TaskEntry{id=4, tipo="calcular", prioridade=1}

[PRODUTOR] write: TaskEntry{id=5, tipo="calcular", prioridade=1}
[CONSUMIDOR-1] Processamento concluído: tarefa 4
[CONSUMIDOR-1] take: TaskEntry{id=5, tipo="calcular", prioridade=1}

[PRODUTOR] Todas as tarefas depositadas. Encerrando.
[CONSUMIDOR-1] Processamento concluído: tarefa 5

[CONSUMIDOR-1] Nenhuma tarefa de alta prioridade disponível. Buscando qualquer tarefa...
[CONSUMIDOR-1] take: TaskEntry{id=1, tipo="calcular", prioridade=2}
[CONSUMIDOR-1] Processamento concluído: tarefa 1

[CONSUMIDOR-1] Nenhuma tarefa de alta prioridade disponível. Buscando qualquer tarefa...
[CONSUMIDOR-1] take: TaskEntry{id=3, tipo="calcular", prioridade=2}
[CONSUMIDOR-1] Processamento concluído: tarefa 3

[CONSUMIDOR-1] Nenhuma tarefa de alta prioridade disponível. Buscando qualquer tarefa...

```

2. O produtor precisou ser modificado para que isso funcionasse?

> _Resposta:_ Não. Apenas o consumidor precisou selecionar as tarefas de acordo com a prioridade.

3. Como o consumidor consegue selecionar apenas tarefas de uma prioridade específica? Qual mecanismo do espaço de tuplas torna isso possível?

> _Resposta:_ Utilizando um template com o campo de prioridade preenchido. O mecanismo de correspondência por templates (matching) seleciona apenas as tuplas compatíveis.

---

### 2.2 Modificação B — Serviço monitor

1. Qual operação você usou no monitor — `read()` ou `take()`? Por quê a outra seria problemática?

> **Resposta:** Foi utilizada a operação read(), pois ela permite observar as tarefas presentes no espaço sem removê-las. Se fosse utilizada take(), o monitor retiraria as tarefas do espaço, impedindo que os consumidores as processassem.

2. Como você contou as tarefas pendentes usando apenas `read()`? Que limitação isso revela?

> **Resposta:** O monitor utiliza read() para verificar se existe uma tarefa disponível sem removê-la do espaço. Essa abordagem evidencia uma limitação do modelo: o espaço de tuplas não possui uma operação nativa de contagem (count()), e apenas com read() não é possível contar corretamente todas as tarefas, pois a mesma entrada pode ser retornada em chamadas sucessivas.

3. Por que um espaço de tuplas puro não tem operação `count()`? O que seria necessário adicionar ao modelo para suportá-la?

> **Resposta:** Porque o modelo Linda oferece apenas operações básicas de escrita, leitura e remoção de tuplas. Para suportar uma contagem correta seria necessário adicionar uma operação específica de consulta ou manter metadados auxiliares sobre as tuplas armazenadas.

Cole o trecho do `Monitor.java` que você completou:

```java
private static int contarTarefas(JavaSpace space) throws Exception {
    TaskEntry template = new TaskEntry(null, null, null);
    int count = 0;

    TaskEntry encontrada = (TaskEntry) space.read(template, null, 0);

    if (encontrada != null) {
        count++;
    }

    return count;
}
```

---

## Observações livres

_(Comportamentos inesperados, erros encontrados, dificuldades técnicas — descreva o que aconteceu e como você resolveu)_

>

---

## Dúvida para a próxima aula

_(Formule uma pergunta substantiva que surgiu durante a atividade)_

>
