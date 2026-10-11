# Winlator 7.1 — Compatibilidade e otimizações Mali

Esta branch adapta **o fork 7.1** para escolher um caminho gráfico seguro por padrão em GPUs que não sejam Qualcomm Adreno. Ela **não instala nem inclui** drivers Vulkan novos.

## Mudanças implementadas

1. **Detecção EGL confiável:** contexto OpenGL ES 2.0 com pbuffer 1×1, liberação de contexto/superfície/display e espera limitada a dois segundos. Evita o `wait()` sem limite da implementação anterior e evita armazenar o nome de uma GPU vazia.
2. **Perfis iniciais dependentes da GPU:** novos contêineres em Adreno mantêm Turnip + DXVK; Mali e outras GPUs não identificadas como Adreno iniciam em **VirGL + WineD3D**. Configurações já salvas não são modificadas.
3. **Variáveis de ambiente seguras para VirGL:** sem `TU_DEBUG`, `ZINK_DEBUG` ou preferências de apresentação Vulkan/Turnip no perfil inicial universal. Cache Mesa de até 256 MB (valor proposto, não medido), `WINEESYNC=1`.
4. **Ajustes de runtime isolados:** o `TU_DEBUG=sysmem` só é adicionado no ambiente de execução do Turnip em Adreno não-6xx, sem regravar o perfil do contêiner; `MESA_NO_ERROR=1` deixou de ser forçado para VirGL. Cada um ainda pode ser configurado explicitamente pelo usuário.

## Métodos gráficos disponíveis nesta base (7.1)

| Caminho | Hardware | Observações |
| --- | --- | --- |
| **VirGL + WineD3D** | Mali e outras GPUs compatíveis com OpenGL ES | Recomendado como primeiro teste em Mali; prioriza estabilidade/compatibilidade |
| **VirGL + CNC DDraw** | Mali e outras GPUs | Alternativa para alguns jogos 2D/DirectDraw antigos |
| **Turnip + DXVK / VKD3D** | Adreno com Vulkan suportado | Não é driver Mali. Evite selecionar em Mali |

**Importante:** `DXVK` traduz Direct3D para Vulkan. No Winlator 7.1 deste fork, VirGL não fornece ao DXVK um backend Vulkan completo para Mali. Não basta alterar a lista do seletor para habilitar DXVK em Mali.

As versões mais recentes do projeto oficial adicionaram:
- **Vortek:** camada Vulkan experimental com suporte a determinados dispositivos Mali (a partir do Winlator 10.0).
- **Gladio:** alternativa para OpenGL via OpenGL ES (a partir do Winlator 11.0).
- **PanVK:** desenvolvimento externo experimental para alguns chips Mali. Sua disponibilidade depende do modelo, das interfaces do kernel e de um pacote realmente compatível. **Não** é ativado por esta branch.

Para usar Vortek e Gladio, será necessário portar/sincronizar o fork para a arquitetura do Winlator atual, não apenas importar um arquivo de configuração. Confira [o projeto oficial](https://github.com/brunodev85/winlator).

## Como testar

1. Instale um APK de teste compilado desta branch, fazendo backup de contêineres e jogos.
2. Em um aparelho Mali (por exemplo, Mali-G68), crie um **novo** contêiner. Confira se o driver inicial é **VirGL** e o wrapper é **WineD3D**.
3. Abra um aplicativo Direct3D 9 simples e depois jogos mais exigentes. Registre tela preta, falhas de shader, tempo de abertura, temperatura e FPS.
4. Para jogos antigos DirectDraw, experimente **CNC DDraw** no lugar de WineD3D.
5. Confira um aparelho Adreno: a criação de contêiner deve continuar com **Turnip + DXVK**.
6. Confira que editar contêineres já existentes **preserva** driver e wrapper escolhidos; que as configurações `TU_DEBUG` não são modificadas permanentemente; e que uma sonda EGL com erro não bloqueia a tela indefinidamente.

## Limitações e métricas

- Ainda não há APK compilado ou medições de FPS/temperatura neste PR.
- O fork-base anuncia Winlator **7.1**; já o projeto oficial disponibiliza **11.2**. Este patch não incorpora as mudanças arquiteturais dessas versões.
- Em Mali, melhor compatibilidade não significa mais FPS. VirGL pode ser mais lento que um backend Vulkan funcional.
- O ganho no tempo da detecção EGL é de **limite de espera**, não um ganho de desempenho 3D.
- Teste ao menos 15 minutos em jogo para observar throttling térmico, stutter, artefatos e queda de desempenho.

## Referências

- [Winlator](https://github.com/brunodev85/winlator)
- [Código do Winlator atual](https://github.com/brunodev85/winlator-app)
- [Documentação Panfrost/PanVK do Mesa](https://docs.mesa3d.org/drivers/panfrost.html)
