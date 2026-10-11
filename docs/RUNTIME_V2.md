# Runtime v2 — Wine primeiro, compatibilidade sem promessas

Esta etapa altera a direção do Winlator Enhanced: a prioridade é uma experiência utilizável e confiável com **Wine + Box64**, documentação objetiva e detecção de falhas. FEXCore/ARM64EC e Proton derivados continuam disponíveis para desenvolvimento, porém sem sugerir que basta extrair uma build Bionic para rodar em um ambiente glibc.

## Problemas encontrados na implementação anterior

A instalação era encaminhada ao importador de Wine 11.2, que apenas tentava descompactar TAR XZ, procurar `bin/wine` e executar `wine --version`. Se isso não funcionasse, o aplicativo exibia uma mensagem genérica. Este caminho não adapta bibliotecas Android/Bionic ao rootfs Linux/glibc.

Os releases de [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases) disponibilizam `.wcp` (TAR Zstandard) para GameNative e `.wcp.xz` (TAR XZ) para forks Winlator CMOD/Ludashi. **Eles não afirmam ser compatíveis com o Winlator 11.2 oficial.**

Outro problema relatado foi o gerenciador Wine não aparecer. A versão anterior deixou o código de um novo menu apenas em branches experimentais; quem instalou uma build de `main` ou o APK original não recebeu essas mudanças. Além disso, a tela Wine original era controlada por `DEBUG_MODE`. O novo build inclui um menu acessível `Wine & Proton`, botão na Biblioteca e seção visível em Configurações.

## Melhorias realizadas no importador

- Reconhecimento pelo conteúdo do arquivo: **XZ e Zstandard**, independente da extensão.
- Extração TAR dedicada, com checagem de caminhos, `..`, symlinks que escapam, arquivos especiais e limites de tamanho/quantidade. A extração geral de outros componentes do Winlator não é modificada.
- Depois de extrair, pré-verificação de `bin/wine` e ELF para indicar se Wine x86_64 Bionic, ARM64EC sem FEXCore ou Linux/AArch64 incompatível foi selecionado.
- Se a pré-verificação falhar, uma janela explica o tipo de incompatibilidade. Se a execução `wine --version` falhar, o diagnóstico indica possível dependência Unix/ABI. Não instala silenciosamente uma distribuição incompatível.
- Suporte a compartilhamento **sob controle do usuário** de um diagnóstico local com informações básicas do Android, Wine, Box64 e FEX.

### O que ainda não está resolvido

**Compatibilidade de execução não é igual à compatibilidade de arquivo**. O importador pode ler um pacote `.wcp.xz`, mas isso não significa que seu `wine` possa ser executado no rootfs. Proton Wine Bionic exige um ambiente de execução adequado, bibliotecas Bionic/Unix correspondentes, configuração Wineprefix e, para ARM64EC, FEXCore. O pacote `Proton-Wine-ARM64EC-FEXCore-Winlator` gerado pela CI da fase anterior também não foi testado em aparelho real e **não é oficialmente funcional**. Usar Wine Linux/glibc destinado ao Winlator 11.2 é o caminho seguro no momento.

## Interface mais discreta

A Biblioteca exibe uma lista simples em fundo grafite, busca e atalho para gerenciar Wine. O gerenciador mostra primeiro as versões instaladas, depois importação, pacotes de teste e criação de contêineres. Foram retirados gradientes fortes, slogans e cartões desnecessários; preservaram-se IDs, navegação e as configurações originais de jogos.

## Open source e próxima direção

O código existente do projeto aproveita **Wine**, **Box64**, **Mesa**, **DXVK**, **VKD3D-Proton** e código **FEX-Emu** compilado separadamente. A próxima prioridade deve ser validar um runtime Proton/Wine com ABI correta, implementar testes automáticos em Android físico e separar a distribuição de Wine por `glibc-x86_64` e `bionic-arm64ec` com uma matriz de compatibilidade publicada. Não copiar recursos ou código proprietários do GameHub.

## Como testar

1. No GitHub Actions, escolha o build da branch `feature/minimal-runtime-manager-v2` com verificação e Gradle aprovados.
2. Faça backup dos seus dados e instale o APK debug, que pode exigir desinstalação da release com outra assinatura.
3. Abra **Wine & Proton** no menu lateral, ou **Biblioteca → Gerenciar Wine e Proton**.
4. Em **Diagnóstico**, confira as versões que o aplicativo reconhece.
5. Tente importar um pacote **Wine x86_64 Linux/glibc compatível com Winlator** para testar o fluxo. As builds Proton Bionic receberão orientação sobre incompatibilidade em vez de uma mensagem genérica.
6. Só depois teste componentes experimentais em um contêiner novo, sem modificar prefixos existentes.

O resultado da CI comprova compilação do APK e testes unitários; não substitui testes de execução no celular.
