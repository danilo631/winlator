# Como encontrar, baixar, instalar e trocar Wine no Winlator Enhanced

## Onde fica o menu?

**Instale primeiro o APK construído da branch `feature/runtime-center-navigation-11`.** É uma versão experimental; menus novos não existem no APK oficial nem nos builds anteriores.

Abra o Winlator Enhanced, toque em **☰** e entre em **Wine & Proton**. Você também encontrará **Wine & Proton • Baixar, instalar e configurar** no cabeçalho da biblioteca de contêineres.

Nesta tela existem três sessões identificadas por número:

1. **Baixar Wine / Proton:** botões para os pacotes **Wine Type** `proton-wine-11.0-2-x86_64.wcp.xz` e `proton-wine-11.0-2-arm64ec.wcp.xz`, que abrem o navegador na URL oficial. **Ver outras versões oficiais** lista os releases GameNative.
2. **Instalar e gerenciar:** **Importar Wine ou Proton baixado** abre imediatamente o seletor de arquivos Android, usando a rotina original de instalação Wine do Winlator. **Gerenciar versões Wine instaladas** abre Configurações já focadas na seção Wine. O gerenciador lista as instalações locais e avisa se encontra módulos FEXCore.
3. **Ativar para um jogo:** **Criar contêiner com Wine escolhido** abre a tela de novo contêiner, onde aparece o campo **Wine** depois de instalar uma versão adicional.

Também é possível usar **☰ → Configurações**. No topo da tela há um painel **Wine, Proton e compatibilidade**, com botões **Abrir Wine & Proton** e **Instalar Wine / Proton baixado**. A seção detalhada **Wine** continua no fim das configurações, mas agora fica visível sem depender de `DEBUG_MODE` (ela era escondida no Winlator 11.2).

## Escolher Box64 ou FEXCore

| Tipo Wine | Tradução indicada | Observações |
|---|---|---|
| Wine x86_64 **para rootfs glibc compatível** | Box64 | Caminho já oferecido pelo Winlator; prefira testar aqui primeiro |
| Proton Wine x86_64 **Bionic** | Box64 é o tradutor x86_64, mas a ABI pode ser incompatível | Um arquivo TAR XZ válido não garante que a build funcione no rootfs |
| Proton Wine Android ARM64EC **com FEXCore** | FEXCore | Exige DLLs PE, bibliotecas Unix, Wineprefix e ABI compatíveis; experimental |

Para selecionar o tradutor, abra um contêiner novo, vá à aba **Variáveis de ambiente**, toque em **CPU / Wine runtime: Box64 or FEXCore** e salve. Ali também estão os perfis **Proton Wine: sincronização e compatibilidade** para ESYNC/NTsync, disponíveis apenas se o Wine correspondente reconhecer as variáveis.

**Por que a versão de Wine não pode ser trocada em um contêiner existente?** O instalador upstream protege o prefixo criado para uma distribuição Wine específica. A criação de outro contêiner é a opção segura até implementarmos uma migração explícita com backup.

## Perguntas frequentes

**Baixei Proton mas ele não aparece na lista:** o download apenas salva o arquivo no dispositivo. Toque em **Importar Wine**, selecione o arquivo, confirme a instalação e aguarde a criação do Wineprefix. Se a importação não concluir, o pacote pode não ser reconhecido pelo Winlator 11.2; tente um Wine x86_64 comprovadamente compatível.

**Por que não tem botão para baixar Steam Proton completo?** O Proton Linux distribuído com Steam não substitui diretamente Wine Android. Este app oferece links para builds Wine Proton de terceiros e não reproduz a instalação Steam Linux.

**Qual o formato certo?** `.wcp.xz` Wine Type (tar.xz); **não** `.wcp` Proton Type (tar.zst). O instalador oficial Wine do Winlator usa extração XZ.

**FEXCore aparece como pré-verificação OK, posso rodar?** Isso confirma somente formato básico do binário Android e presença das DLLs. Não comprova dependências Unix, Vulkan, compatibilidade do Wineprefix ou inicialização real no aparelho.

**Onde baixar o APK certo?** [GitHub Actions — Enhanced Android](https://github.com/danilo631/winlator/actions/workflows/enhanced-android.yml). Escolha a execução da branch `feature/runtime-center-navigation-11`, verifique sucesso e abra **Artifacts → Winlator-Enhanced-11-debug**. Faça backup antes de instalar.

**Como voltar ao Box64?** Em novo contêiner, selecione uma build Wine x86_64 compatível e escolha Box64 como tradutor. Mudar somente a opção do CPU mantendo Wine ARM64EC não resolve incompatibilidades.
