# Winlator Enhanced 11.2 — Etapa 4: biblioteca moderna + Proton Wine

## O que foi implementado

1. **Nova biblioteca de contêineres**: interface escura com cartões arredondados, cabeçalho em gradiente, busca por nome, resolução, driver gráfico, versão Wine, wrapper DirectX e variáveis de runtime.
2. **Informações por contêiner**: identifica o motor escolhido (Box64 ou FEXCore), driver, resolução e versão Wine selecionada. Não inventa capa do jogo, FPS ou compatibilidade.
3. **Atalhos de ação preservados**: iniciar contêiner e acessar seu menu continuam nas IDs originais. Sem copiar a interface ou componentes proprietários do GameHub.
4. **Proton Wine no menu Configurações → Wine**: botão “Proton Wine 11 • versões para Android”. Mostra diferenças entre **Wine Type `.wcp.xz`**, `Proton Type `.wcp` e como importar um release pelo seletor de arquivos existente no Winlator.
5. **Parâmetros opcionais de sincronização** para builds Proton Wine Bionic com suporte a NTsync: Automatic, userspace NTsync, disable NTsync ou Wine ESYNC. Somente as variáveis correspondentes são alteradas e todo resto é preservado.
6. **FEXCore + Wine ARM64EC**: herda a integração experimental da etapa 3, com ajuste no `LD_LIBRARY_PATH` para localizar as bibliotecas do Wine nativo e não adicionar `BOX64_LD_LIBRARY_PATH` quando FEX estiver ativo. A seleção é individual por contêiner/atalho, com preflight e ausência de fallback silencioso.
7. **Upstream sync protegido**: layout dos contêineres é substituído apenas se seu conteúdo for exatamente igual ao blob do submódulo fixo Winlator 11.2. Demais resources novos não sobrescrevem assets upstream. `--check` e reaplicação repetida testadas pela CI.

## Como instalar Proton Wine 11 no Winlator Enhanced

O projeto aberto [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases) publicou em **10/10/2026** o release `proton-11.0-2-20261010`. O workflow público gera dois formatos:

- `proton-11.0-2-<arch>.wcp`: **tar.zst**, descrito pelo projeto como *Proton Type* para GameNative; o instalador oficial Winlator 11.2 só sabe extrair TAR XZ, não confunda os tipos.
- `proton-wine-11.0-2-<arch>.wcp.xz`: **tar.xz**, descrito pelo projeto como *Wine Type* para outros forks Winlator. O workflow upstream mostra que ele contém `bin`, `lib`, `share`, `profile.json` e `prefixPack.txz`.

No Winlator Enhanced, vá a **Configurações → Wine → Proton Wine 11 • versões para Android → Ver releases**. Baixe a variante **Wine Type (`.wcp.xz`)** e use **Escolher arquivo**, que abre o instalador Wine existente do Winlator. Ele já extrai arquivos tar.xz.

**Atenção**: equivalência de **formato** não garante compatibilidade de **runtime/ABI**. Um Wine x86_64 Bionic pode não funcionar em um rootfs glibc preparado para Box64; builds ARM64EC exigem um Wine Android/Bionic, FEXCore compatível, unixlibs e os recursos do Wineprefix. Se a sondagem Wine falhar, não force o app a substituir sua instalação. Faça backup.

### Ativação por contêiner

1. Selecione uma versão Wine compatível nas configurações do contêiner.
2. Na aba Environment Variables, mantenha **Box64** para Wine Linux x86_64, ou selecione **FEXCore** somente com uma distribuição nativa Wine ARM64EC apta e compatível com FEX.
3. Use **Proton Wine: sincronização e compatibilidade** para testar `automatic`, `userspace NTsync`, `disable NTsync` ou `ESYNC`. Salve o contêiner. Opções NTsync específicas só funcionam quando implementadas no Wine Proton instalado.
4. Compare tempos de inicialização, FPS/frametimes e estabilidade. Desfaça um ajuste isoladamente em caso de regressão.

#
## Combinar Proton Wine ARM64EC e FEXCore automaticamente (no computador)

A etapa 3 já disponibiliza módulos FEXCore compilados no GitHub Actions. Agora a ferramenta `scripts/assemble_fex_proton.py` combina arquivos reais:

- **Entrada Wine:** `proton-wine-11.0-2-arm64ec.wcp.xz` (Wine Type, formato tar.xz) do release oficial GameNative.
- **Entrada FEX:** `FEXCore-2609-ARM64EC-modules.tar.xz`, extraído do artefato do build FEXCore da etapa 3.
- **Saída:** `proton-fex-winlator.tar.xz` e `proton-fex-winlator.tar.xz.sha256`, prontos para tentativa de importação no instalador Wine do Winlator Enhanced.

Com Python 3.10+ no computador:

```bash
python3 scripts/assemble_fex_proton.py \
  --wine proton-wine-11.0-2-arm64ec.wcp.xz \
  --fex FEXCore-2609-ARM64EC-modules.tar.xz \
  --output proton-fex-winlator.tar.xz
```

O montador verifica cabeçalho ELF64/AArch64 **com interpretador Bionic Android**, assinatura PE das DLLs, presença dos dois módulos FEX e segurança dos caminhos dos arquivos TAR. Preserva os demais componentes originais do Wine, substitui os módulos FEX na árvore e calcula SHA-256. Rejeita Linux/glibc, arquivos incompatíveis e tentativas de escapamento do arquivo compactado.

**Atenção:** módulos FEXCore PE compilados separadamente **precisam ser compatíveis com o Wine ARM64EC escolhido**. A montagem não cria as bibliotecas Unix do FEX/Proton nem resolve automaticamente diferenças de ABI, Wineprefix e drivers. Por isso o resultado é um **pacote de integração experimental**, não uma confirmação de jogos funcionando.


### Sem computador: montar diretamente pelo GitHub Actions

O workflow **Bundle ARM64EC Proton Wine + FEXCore** (`.github/workflows/proton-fex-bundle.yml`) faz a montagem automaticamente: baixa o release oficial Proton Wine ARM64EC do GameNative, localiza o último build bem-sucedido das DLLs FEXCore na mesma conta/repositório, verifica ELF/PE e segurança do TAR, monta o pacote e calcula SHA-256. Ele não instala nada no seu Android e não modifica o aplicativo.

Abra a aba **Actions** do seu fork, escolha **Bundle ARM64EC Proton Wine + FEXCore**, clique em **Run workflow** e selecione a branch `feature/launcher-proton-experience-11`. Após uma execução **bem-sucedida**, procure em **Artifacts** o arquivo `Proton-Wine-ARM64EC-FEXCore-Winlator`. Extraia seu ZIP e use o `.tar.xz` dentro dele no instalador Wine.

A execução usa o tag Proton Wine `proton-11.0-2-20261010` e o asset `proton-wine-11.0-2-arm64ec.wcp.xz` por padrão; ambos podem ser informados manualmente. O artefato das DLLs FEXCore precisa estar disponível e não expirado (retenção atual de 14 dias). A montagem pode falhar caso a estrutura da distribuição de terceiros mude, contenha symlinks inseguros, não traga o interpretador Android esperado ou não seja compatível com essa combinação de módulos.

**Crítico:** mesmo um build e uma importação bem-sucedidos não comprovam que Wine ARM64EC e FEXCore funcionarão sobre o rootfs do Winlator 11.2. Não remova seu contêiner Box64, faça backup e teste a nova combinação em um contêiner separado.

## Segurança e desempenho

- Não altera clocks/governor nem desativa proteção térmica.
- Não injeta arquivos proprietários GameHub nem componentes fechados.
- Busca local; nenhuma informação dos jogos é enviada à rede.
- O atalho de download abre somente a página oficial GitHub da fonte, sem download silencioso.
- APK **debug** poderá conflitar com o Winlator instalado. Salve/exporte seus contêineres antes de instalar.
- Não declara suporte Steam/DRM, desempenho Mali, renderização Vulkan avançada ou jogos FEX realmente validados.
- Os ganhos de compatibilidade e FPS dependem da build Wine/driver/GPU e exigem testes em dispositivo Android.

## Arquitetura e arquivos

- `overrides/app/src/main/res/layout/{containers_fragment,container_list_item}.xml` — UI.
- `overrides/app/src/main/res/drawable/enhanced_*.xml` — visual.
- `EnhancedLibraryUI.java`, `LibraryFilter.java` — metadados e busca.
- `ProtonSourcesUI.java` — download oficial + fluxo SAF para instalação.
- `ProtonTuningUI.java`, `ProtonSyncRules.java` — perfis de sincronização.
- `scripts/apply_app_overrides.py` — patcher fail-closed para submódulo Winlator 11.2.
- `.github/workflows/enhanced-android.yml` — Java tests, Python tests, build Android e artefato APK.

## Fontes abertas

- [GameNative Proton Wine releases — builds Bionic/WoW64](https://github.com/GameNative/proton-wine/releases)
- [GameNative CI producing .wcp (ZSTD) and .wcp.xz (XZ)](https://github.com/GameNative/proton-wine/blob/proton_11.0-2/.github/workflows/build-proton.yml)
- [ValveSoftware/Proton](https://github.com/ValveSoftware/Proton)
- [FEX-Emu/FEX](https://github.com/FEX-Emu/FEX)
- [DXVK](https://github.com/doitsujin/dxvk)
- [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton)

## Validação ainda necessária

Testar o APK num Android real, especialmente a navegação em layouts, seleção de Wine, importação real de release `.wcp.xz`, iniciar Wine ARM64EC/FEXCore e executar jogos com Vortek/Gladio ou outros drivers Mali. O status desta etapa deve permanecer **draft** até esses testes.
