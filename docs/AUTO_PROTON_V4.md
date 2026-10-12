# Winlator Enhanced v4 — Proton automático e auditoria do Wine

## O que está disponível

Menu **☰ → Wine & Proton → Buscar e instalar Proton (GitHub)**:

1. O app consulta a API pública de releases do repositório [Kron4ek/Wine-Builds](https://github.com/Kron4ek/Wine-Builds/releases), por HTTPS e em uma thread de trabalho.
2. A lista oferece o **Wine com patches Proton para Linux AMD64 WoW64** e o Wine Staging Linux, agrupando releases estáveis recentes. É um catálogo dinâmico, não uma URL de versão fixada no app.
3. Depois de tocar em uma versão, o usuário confirma a origem, tamanho e instalação. O aplicativo baixa o arquivo, mostra progresso e permite cancelar.
4. A validação confere **nome do pacote, tag do release, URL da fonte oficial, tamanho exato do arquivo e SHA-256 informado no release**. Links de download são restritos a HTTPS e a redirecionamentos pelos domínios oficiais de assets do GitHub. Downloads sem SHA-256 publicado não são instalados automaticamente.
5. O pacote é encaminhado para o **instalador Wine já existente do Winlator** sem exigir escolher um arquivo manualmente. O instalador descompacta de modo isolado, detecta `bin/wine`, analisa ELF/ABI e tenta `wine --version`; se der certo, começa a inicialização do Wineprefix e o reinício do aplicativo como no fluxo original.
6. No fim, use **Instalados** para verificar a versão; crie um contêiner novo e escolha o Wine instalado.

A importação pode falhar se a build Linux tiver dependências diferentes do rootfs. **Não é o Steam Proton completo**: o Wine com patches Proton desse repositório não inclui automaticamente DXVK ou VKD3D-Proton. O Winlator possui opções próprias para essas camadas.

## GameNative vs. Kron4ek

O projeto [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases) publica Wine Type `.wcp.xz` para ambientes **Android/Bionic**, e as variantes `.wcp` usadas no GameNative. Mesmo o `.wcp.xz` dessa origem **não** é automaticamente compatível com o rootfs **Linux/glibc** do Winlator 11.2. Essas versões **não entram no instalador automático**, embora continuem acessíveis para pesquisa e experimentos manuais.

O catálogo automático escolhe como ponto de partida builds **Linux/glibc** de [Kron4ek/Wine-Builds](https://github.com/Kron4ek/Wine-Builds/releases) porque estão mais próximas da pilha Box64/glibc. Ainda assim precisam de teste real.

## Auditoria de código e bugs corrigidos

| Área | Antes | Agora |
|---|---|---|
| Instalação | Exigia navegador e seletor de arquivos, sem verificação de integridade | GitHub API → escolha → download → SHA-256 → importação Wine → prefixo |
| Regressão de ABI | Pacotes Android/Bionic apresentavam erro genérico | Bloqueio explícito de auto-instalação de Bionic; diagnóstico do Wine extraído |
| Libs de arquivos | AppCompat 1.4.0, Zstd JNI 1.5.2-3, XZ 1.7, Commons Compress 1.20 | AppCompat 1.7.1, Zstd JNI 1.5.7-13, XZ 1.10, Commons Compress 1.28.0 |
| Pastas Wine | Diretórios temporários/incompletos podiam aparecer na lista de versões | Somente diretórios `wine-*` com `bin/wine` válido são incluídos |
| Fluxo de download | Não existia cancelamento ou checagem de tamanho | Limites de tamanho, espaço livre, arquivo `.part`, hash, cancelamento, limpeza em falha |
| Riscos TAR | O extrator geral do upstream não impunha checagem de caminhos | Extração Wine dedicada com limites e proteção de caminho e symlinks |
| Interface | Botão de download abria release antigo no navegador | Pesquisa dinâmica dentro do app, com versões acessíveis e avisos de compatibilidade |

**Decisão sobre dependências:** foram atualizados componentes Java do aplicativo relacionados ao fluxo e à descompactação, sem trocar o projeto NDK ou submódulos nativos. Atualizar Mesa, DXVK, Vortek, Box64, FEX e Wine de forma cega poderia introduzir regressões graves. Essas bibliotecas precisam de compilação, verificação de licença, ABI e execução específicas antes de entrar na `main`.

## Testes e limitações

- Testes Java puros de política de origem, nomes, versões, SHA-256, links e tamanho.
- Testes anteriores de ELF, cabeçalhos XZ/Zstandard, restrições de caminho TAR, GPU e perfis.
- Testes Python de scripts e presença do fluxo no layout.
- Aplicação idempotente dos overlays da versão 11.2 e compilação Android debug no GitHub Actions.
- **O APK compilar não prova que o Wine-Proton glibc abrirá jogos**, que o Wineprefix será concluído em todos os dispositivos, ou que Vulkan/DXVK/VKD3D estarão disponíveis. São exigidos testes físicos em GPUs Mali e Adreno.

O instalador automático requer internet, acesso à API pública do GitHub, armazenamento livre para o download e extração e os arquivos de sistema do Winlator instalados. O usuário precisa manter o app aberto durante o download. O app **não** solicita acesso root, ADB ou permissões globais de instalação de pacotes Android.

## Diagnóstico de uma falha

Abra **☰ → Wine & Proton → Diagnóstico**. Se o GitHub retornar HTTP 403, pode ser o limite de chamadas anônimas; tente mais tarde. Se ocorrer erro SHA-256, o arquivo baixado é rejeitado. Se a extração e a verificação ELF passarem, mas `wine --version` falhar, será exibida uma mensagem indicando ABI ou biblioteca Unix. Em todos os casos, as versões Wine anteriormente instaladas devem permanecer intactas.
