# Winlator Enhanced — Native GPU / Wine Manager v3

## Pesquisa técnica e escolha arquitetural

O Winlator 11.2 oficial já contém a ponte gráfica **Vortek**, com componente Java/C++ nativo `VortekRendererComponent`: ela executa uma biblioteca nativa Android (`libvortekrenderer`), conecta os processos de jogo por socket local e inicializa uma ponte Vulkan que recebe o caminho do driver `Adrenotools`. O controle **System** do seletor de drivers faz o módulo usar o driver do próprio Android, sem instalar Turnip. Portanto, para GPUs Mali, Xclipse e PowerVR, **não é necessário inventar um driver Vulkan**: utilizamos a infraestrutura open source de Vortek que já existe no Winlator.

Isso não quer dizer que um executável Windows envie DirectX diretamente ao Vulkan do Android. O caminho real continua **Windows → Wine → DXVK/VKD3D/WineD3D → Vulkan/OpenGL → ponte Vortek → driver Android**. É preciso compatibilidade da API e das extensões Vulkan, em especial para DX12.

Pesquisas abertas:
- [Winlator 11.2, GraphicsDrivers, GraphicsDriverPicker e VortekRendererComponent](https://github.com/brunodev85/winlator).
- [leegao/bionic-vulkan-wrapper](https://github.com/leegao/bionic-vulkan-wrapper): trabalho aberto de wrapper Vulkan para Winlator Bionic, com tratamento especial de extensões e formatos BCn/particularidades Mali, **não incorporado a esta build glibc** sem validação de ABI e licença.
- [WSI Woes on Mali](https://leegao.github.io/winlator-internals/wrapper/2026/07/28/wsi-woes-mali.html): documenta problemas específicos de imagem de swapchain e cópias em dispositivos Mali. Usar o driver nativo não elimina automaticamente esses gargalos.
- [GameNative Open Source](https://github.com/GameNative): Wine, DXVK, Mesa, FEX, NTsync e outros componentes com licenças independentes.
- [FEX-Emu](https://github.com/FEX-Emu/FEX) e [Mesa 3D](https://mesa3d.org): componentes abertos e sujeitos a requisitos ABI.

Não há evidência no código verificado para prometer que todo fork GameHub bloqueia automaticamente substituições no driver, ou que aumentar frequência via ADB/root seja seguro/necessário. **Este fork não altera GPU governor, frequências, arquivos de sistema ou settings globais por ADB**.

## Implementação

`NativeDriverPolicy.java` classifica o nome do renderizador EGL e seleciona, de forma conservadora, **Vortek** para Mali/Immortalis, Exynos Xclipse e PowerVR; somente Adreno com identificador 6xx/7xx recebe **Turnip** por padrão. Adreno 8xx/indeterminado recebe Vortek até confirmação de compatibilidade. A decisão é testada em Java puro.

`XServerDisplayActivity` aplica o **guardião na inicialização do jogo**, depois de ler as configurações do contêiner e do atalho: se a proteção estiver ligada e a GPU for Mali, Xclipse ou PowerVR, o driver Vulkan efetivo será Vortek e `adrenotoolsDriver` será `System`. **Os dados salvos do usuário não são modificados**, e o guardião pode ser desligado em **Configurações → Wine e Proton → Proteção automática do driver**. Desconhecidos e Adreno mantêm a configuração manual. O código não esconde crashes de Vulkan que ocorram por extensões ou shaders faltantes.

A versão Wine **é um assunto separado do driver gráfico**: Bionic e glibc são ABIs distintas. O suporte do importador a XZ/Zstd ou a presença das DLLs FEXCore não converte uma distribuição GameNative Bionic em Wine Linux/glibc executável.

## Gerenciador Wine direto

Menu **☰ → Wine & Proton** → **Instalados**: cada Wine realmente instalado aparece como botão. Toque nele para ver a arquitetura pré-verificada, contêineres que o utilizam e opção de **remover**, com confirmação. A versão integrada do Winlator é protegida; a remoção é bloqueada se um contêiner usar a versão, se o Wine não estiver dentro da pasta conhecida ou se não houver modelo de prefixo instalado. A exclusão de arquivos acontece fora da thread da interface; a lista é atualizada ao terminar.

O botão **Atualizar versões** força nova leitura. **Importar pacote Wine** usa o seletor de arquivos original. **Criar contêiner** permite selecionar uma versão adicional recém-instalada. Não altere o Wine de um contêiner existente sem migrar o prefixo.

## Validação e limitações

- Testes unitários classificam GPU e o driver conservador.
- `scripts/apply_app_overrides.py` valida e injeta novas classes e alterações preservando o upstream Winlator 11.2.
- O GitHub Actions verifica idempotência e compila o APK Android.
- **Não há benchmark de jogos em telefone físico; não há promessas de FPS, de DX12 no Mali ou de compatibilidade plena de Proton Bionic.**
- Antes de testar outro APK debug, faça backup dos contêineres; assinaturas incompatíveis podem exigir reinstalação.

## Próxima arquitetura

Prioridade: um caminho Box64/Wine glibc estável com driver Android automático via Vortek, logs verificáveis de extensões Vulkan e UI intuitiva. O caminho Bionic/FEXCore passa a ser uma configuração de runtime separada e versionada, que só será habilitada para jogos depois da validação de ABI e de teste em hardware.
