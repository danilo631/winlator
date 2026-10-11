# Winlator Enhanced 11.2 — Etapa 3: FEXCore ARM64EC e Proton Wine (experimental)

> Status: **integração de código / compilação experimental**. FEXCore e Proton não estão embutidos no APK. Só funcionam quando um Wine compatível e os componentes correspondentes são fornecidos. Não afirmamos testes de execução em Android/Mali.

## Por que esta arquitetura

Winlator 11.2 original executa Wine x86_64 Linux por meio de **Box64**, com bibliotecas glibc e rootfs Linux; o `GuestProgramLauncherComponent` inicia `/usr/local/bin/box64 wine ...`.

Há dois produtos distintos chamados "FEX":

1. **FEXLoader Linux ELF**: emula processos Linux x86/x86_64 sobre Linux ARM64 e depende de Linux syscalls/rootfs. O projeto FEX diz expressamente que **não funciona no Android**.
2. **FEXCore + Wine ARM64EC/WoW64**: módulos PE (`libarm64ecfex.dll`, `libwow64fex.dll`) ligados a um **Wine ARM64 Android/Bionic**. O Wine nativo inicia sem Box64; o código Windows x86 é traduzido pelo FEXCore na ponte WoW64/ARM64EC. Esta é a arquitetura experimental implementada no seletor.

Proton também tem duas formas relevantes: o **Proton Linux/Steam** (não diretamente importável no Android) e um **Wine derivado do Proton** compilado para um runtime compatível, acompanhado dos componentes gráficos DXVK/VKD3D. GameNative publica Wine/Proton Bionic ARM64EC para suas implementações Android, mas isso não significa que suas distribuições `.wcp` sejam compatíveis com o WineInstaller oficial 11.2.

## Código integrado

- **Winlator `GameProfileUI`**: na aba Variáveis de ambiente de contêineres/atalhos, o botão **CPU / Wine runtime: Box64 or FEXCore** salva `WINLATOR_CPU_BACKEND=fexcore` somente quando selecionado. Box64 segue padrão e restaura a opção original.
- **`FexWineRuntime`**: verifica ELF64 little-endian **AArch64**, interpretador Android `/system/bin/linker64` ou APEX, diretórios Wine e as duas DLLs PE. Rejeita ELF x86_64, glibc e comandos arbitrários. Arquivos com nomes corretos não garantem compatibilidade ABI — a validação é necessária, mas não suficiente.
- **`GuestProgramLauncherComponent`**: se o backend for FEXCore, chama diretamente o `bin/wine` ARM64/Bionic, sem o `box64`; preserva variáveis Wine, home, X11 e áudio. Se o runtime não satisfizer o contrato, mostra diagnóstico e recusa execução em vez de iniciar outro backend silenciosamente.
- **`WineInstaller`**: detecta Wine Android ARM64/Bionic importado e o inicializa pelo backend nativo para `--version` e geração do prefixo, sem tentar executá-lo via Box64. Wine x86_64 mantém o fluxo original.
- **`scripts/package_proton_wine.py`**: gera um `.tar.xz` com um Wine pré-compilado na disposição aceita pelo instalador. Valida arquitetura, árvore de arquivos, presença dos módulos FEX, impede links simbólicos perigosos e emite SHA-256.
- **CI**: testes sintéticos dos cabeçalhos ELF e DLLs, bloqueio de comandos nativos indevidos, layouts Proton Wine e builds APK.

## Como testar com componentes que você tem direito de usar

### Opção A: Proton Wine x86_64 + Box64

1. Obtenha um **Wine x86_64 pré-compilado compatível com o rootfs glibc** do Winlator (pode ser derivado do Wine usado no Proton; não confundir com o script de inicialização do Steam).
2. Extraia a pasta que contém `bin/wine`, `lib/wine` etc.
3. Em computador Linux com Python 3:

```bash
python3 scripts/package_proton_wine.py \
  --source /caminho/para/wine-files \
  --output proton-wine-x86_64.tar.xz \
  --backend box64
```

4. Transfira o `.tar.xz` e abra **Settings → Install Wine**. Selecione a versão importada nas configurações do contêiner e mantenha o backend **Box64**.
5. Use DXVK/VKD3D do próprio Winlator; eles não significam que o Proton Steam completo esteja instalado.

### Opção B: Wine Bionic ARM64EC + FEXCore

1. Obtenha uma **árvore Wine ARM64EC feita especificamente para Android/Bionic**, com `bin/wine`, módulos Wine e as DLLs **compatíveis com aquela build**:
   - `lib/wine/aarch64-windows/libarm64ecfex.dll`
   - `lib/wine/aarch64-windows/libwow64fex.dll`
   - para versões novas do FEXCore, os `.so` unixlib correspondentes, quando exigidos pelo Wine usado.
2. Não misture FEXCore glibc com Wine Bionic. Não use um FEXLoader Linux no lugar de `libarm64ecfex.dll`.
3. Gere a distribuição usando:

```bash
python3 scripts/package_proton_wine.py \
  --source /caminho/para/wine-android-arm64ec \
  --output proton-arm64ec-fex.tar.xz \
  --backend fexcore
```

4. Importe com **Settings → Install Wine**, configure o Wine recém-importado no contêiner e, em **Environment Variables → CPU / Wine runtime**, escolha FEXCore.
5. Para reverter, escolha Box64 **e volte para um Wine x86_64 compatível com Box64**. O Wine ARM64EC não roda em Box64; não tente simplesmente trocar o tradutor mantendo Wine incompatível.

### Limitações importantes

- **Sem distribuição automática de binários FEXCore ou Proton**: builds ARM64EC e Android têm licenças, dependências, formatos e ABIs diferentes. O APK não baixa conteúdo do GameHub.
- **Não há garantia de que Wine ARM64EC importado executará no rootfs glibc do Winlator 11.2**: ponte de bibliotecas, Wayland/X11, bionic linkers e comunicação com os componentes nativos necessitam testes no hardware.
- Não é possível afirmar que FEXCore acelera jogos Mali sem medição. Ele traduz **CPU**, não é um driver gráfico Mali.
- Proton requer configurações de prefixo e bibliotecas do seu próprio runtime; um `proton` script da Steam não substitui `wine` no Winlator.
- Faça backup dos contêineres antes de importar Wine, pois cada versão pode alterar o prefixo.

## Próximos passos necessários para execução validada

Construir e testar um par **Wine ARM64EC/Bionic + FEXCore DLLs + unixlib compatível**, verificar inicialização do Wine e leitura de executáveis Windows no Android e integrar distribuição de componentes **por checksum, versão e arquitetura**. Sem isso, a implementação é um *backend experimental com validações reais*, não um runtime FEX pré-instalado e funcional universalmente.

## Fontes oficiais e implementações analisadas

- [FEX FAQ (limitações Android e FEXCore)](https://wiki.fex-emu.com/index.php/FAQ)
- [FEX 2501 WoW64 / ARM64EC](https://fex-emu.com/FEX-2501/)
- [Hangover com Wine ARM64EC](https://github.com/AndreRH/hangover)
- [GameNative Proton Wine Android](https://github.com/GameNative/proton-wine/releases)
- [Box64 / Wine variants](https://github.com/ptitSeb/box64/blob/main/docs/WINE.md)
- [Valve Proton](https://github.com/ValveSoftware/Proton)
- [GameHub Lite API (component format and taxonomy only)](https://github.com/Producdevity/gamehub-lite-api)
