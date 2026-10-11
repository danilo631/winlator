# Roadmap — Winlator Enhanced 11.2

| Entrega | Estado atual | Próxima verificação |
|---|---|---|
| Biblioteca de contêineres e busca | Código e APK debug compilados na fase 4 | Testar layouts em telefones/tablets e leitores de tela |
| Menu Wine & Proton | Implementado nesta etapa | Confirmar navegação/importação no Android físico |
| Visibilidade da configuração Wine | Corrigido: antes oculto por DEBUG_MODE | Testar seleção, instalação e retorno do app |
| Link oficial Proton Wine | Integrado à UI | Conferir funcionamento do download no navegador |
| Importação de Wine Type .wcp.xz | Encaminhada ao instalador TAR XZ original | Validar cada build Bionic/glibc separadamente |
| Wine x86_64 via Box64 | Backend original preservado | Regressão de jogos Windows conhecidos |
| FEXCore ARM64EC | Launcher/compilação PE experimental | Compatibilidade de Wine e unixlib Android e games reais |
| ESYNC/NTsync | Parâmetros opt-in | Medir ganhos, regressões e consumo térmico |
| Monitor de intervalos de atualização | Instrumentado para HUD | Comparar com tempos reais da apresentação Vulkan |
| Estrutura do repositório | README/guia/índice/arquitetura e CI | Revisão e compactação de scripts após maturidade |

## Critérios mínimos antes de marcar como estável

1. Compilação CI e testes estáticos aprovados.
2. Instalação, primeira inicialização e mudança entre Configurações e Contêineres sem travamentos.
3. Importação de Wine e criação de novo contêiner sem modificar prefixos existentes.
4. Um jogo 3D funcional no caminho Box64 e testes repetíveis comparativos (mesmas configurações/condições) para qualquer alegação FEX/Proton.
5. Empacotamento e assinaturas, atribuições de terceiros e estratégia de atualização sem perda de dados.

Não há meta garantida de FPS; o desempenho depende da GPU, driver, Wine, código do jogo e limitações térmicas.
