import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.discovery.LookupLocator;
import net.jini.space.JavaSpace;

/**
 * Observa o espaço de tuplas a cada 3 segundos e exibe quantas tarefas pendentes existem.
 *
 * O monitor NÃO deve remover tarefas do espaço — apenas observar.
 *
 * Nível 2B: localize o comentário TODO abaixo e complete a implementação.
 */
public class Monitor {

    private static final String REGGIE_HOST = "reggie";
    private static final int INTERVALO_SEGUNDOS = 3;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.policy", "security.policy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        JavaSpace space = descobrirEspaco();
        System.out.println("[MONITOR] Espaço encontrado. Iniciando monitoramento a cada "
            + INTERVALO_SEGUNDOS + "s...");

        while (true) {
            Thread.sleep(INTERVALO_SEGUNDOS * 1000);
            int pendentes = contarTarefas(space);
            System.out.println("[MONITOR] Tarefas pendentes no espaço: " + pendentes);
        }
    }

    private static int contarTarefas(JavaSpace space) throws Exception {
        TaskEntry template = new TaskEntry(null, null, null);
        int count = 0;

        // TODO: use read() ou take() aqui — qual você deve usar e por quê?
        //
        // Dica: o espaço de tuplas não tem operação count().
        // Você precisa ler as entradas uma a uma para contá-las.
        // Pense: se você usar take(), o que acontece com as tarefas?
        //
        // Substitua o null abaixo pela chamada correta:
        //
        //   TaskEntry encontrada = (TaskEntry) space.???(template, null, 0);
        //
        // Use timeout = 0 (JavaSpace.NO_WAIT) para não bloquear quando não houver mais entradas.
        //
        // Atenção: read() com timeout=0 pode retornar a mesma entrada mais de uma vez —
        // o Outrigger não garante ordem nem variedade entre chamadas sucessivas.
        // Para contar corretamente, você precisaria de um campo id único por tarefa
        // e rastrear quais ids já foram vistos. Esse problema não tem solução limpa
        // com a API pura do espaço de tuplas — é justamente a limitação que o Nível 2B explora.

        // ===================== ALTERAÇÃO 1 =====================
        // Usa read() para observar uma tarefa sem removê-la do espaço.
        TaskEntry encontrada = (TaskEntry) space.read(template, null, 0);
        // TaskEntry encontrada = null; // ← substitua esta linha

        // ===================== ALTERAÇÃO 2 =====================
        if (encontrada != null) {
            count++;
            //count = 1; // ← implemente a contagem real acima e remova esta linha
        }
        // ======================================================

        return count;
    }

    private static JavaSpace descobrirEspaco() throws Exception {
        LookupLocator locator = new LookupLocator("jini://" + REGGIE_HOST);
        ServiceTemplate template = new ServiceTemplate(null, new Class[]{JavaSpace.class}, null);

        for (int tentativa = 1; tentativa <= 20; tentativa++) {
            try {
                ServiceRegistrar registrar = locator.getRegistrar();
                Object service = registrar.lookup(template);
                if (service != null) {
                    return (JavaSpace) service;
                }
            } catch (Exception e) {
                // reggie ou javaspaces ainda não prontos
            }
            System.out.println("[MONITOR] Aguardando espaço... (" + tentativa + "/20)");
            Thread.sleep(2000);
        }
        throw new RuntimeException("[MONITOR] Não foi possível encontrar o espaço de tuplas.");
    }
}
