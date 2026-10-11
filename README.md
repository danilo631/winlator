<p align="center">
  <img src="logo.png" width="340" alt="Logotipo do Winlator" />
</p>

# Winlator Enhanced — Android

Fork experimental do [Winlator 11.2 original](https://github.com/brunodev85/winlator), dedicado a facilitar o gerenciamento de Wine, explorar Box64/FEXCore e melhorar a experiência de jogos Windows no Android. **Não é uma distribuição oficial do Winlator, Proton, Valve, FEX ou GameHub.**

> ⚠️ **Estado do projeto:** recursos desta branch ainda são experimentais e não foram validados em todos os dispositivos, especialmente GPUs Mali. Um APK compilado não comprova que jogos vão executar. Faça backup dos contêineres antes de instalar uma versão debug.

## Onde encontrar Wine e Proton no aplicativo?

Nas versões Enhanced que incluem esta alteração, abra o menu lateral **☰ → Wine & Proton**. O gerenciador também está acessível pelo botão **Wine & Proton • Baixar, instalar e configurar** na biblioteca inicial.

1. **Baixar:** selecione `Proton Wine x86_64 • Box64` ou `Proton Wine ARM64EC • FEXCore`, ou veja [todos os releases oficiais GameNative](https://github.com/GameNative/proton-wine/releases). Os botões abrem o navegador para baixar um arquivo do tipo **Wine .wcp.xz**; não use `.wcp` (formato diferente) no importador XZ.
2. **Instalar:** toque em **Importar Wine ou Proton baixado** e selecione o arquivo na pasta Downloads. O instalador tentará verificar o Wine e preparar o prefixo.
3. **Selecionar:** clique em **Criar contêiner com Wine escolhido** e escolha a versão Wine no campo **Wine**. Na aba **Variáveis de ambiente**, selecione o backend adequado: **Box64** (x86_64) ou **FEXCore** (Wine Android ARM64EC preparado).
4. **Gerenciar:** acesse **Wine & Proton → Gerenciar versões Wine instaladas** ou **☰ → Configurações → Wine**. A área Wine, escondida no Winlator 11.2 release, foi reabilitada. O Winlator mantém a versão de Wine de contêineres existentes para evitar corromper seus prefixos.

**Atenção:** não misture uma distribuição Bionic Wine com componentes glibc sem verificar compatibilidade. FEXCore ARM64EC exige as DLLs e bibliotecas Unix correspondentes e ainda pode falhar no rootfs do Winlator 11.2.

## Instalar um APK experimental pelo GitHub

O código é construído automaticamente na branch **`feature/runtime-center-navigation-11`** em [GitHub Actions — Winlator Enhanced / Android](https://github.com/danilo631/winlator/actions/workflows/enhanced-android.yml).

1. Abra **Actions**, selecione o build mais recente da branch de gerenciamento Wine e verifique que **Verify overlays** e **Build Android debug APK** estejam verdes.
2. Na parte inferior da execução, vá a **Artifacts → Winlator-Enhanced-11-debug**.
3. Extraia o ZIP e instale o APK debug no Android, após guardar seus dados/contêineres. Um APK debug pode não atualizar diretamente uma versão release assinada de modo diferente.

**A versão padrão do branch `main` não recebe alterações de branches experimentais automaticamente**. O novo menu não estará no APK do Winlator oficial ou em builds antigos do nosso fork.

## Recursos em desenvolvimento

| Área | Estado |
|---|---|
| Biblioteca de contêineres, cartões e busca | Implementada no código; requer teste no aparelho |
| Menu Wine & Proton com diagnósticos e seleção de arquivos | Implementado nesta branch |
| Instalação de Wine x86_64 compatível | Usa o instalador do Winlator; depende da distribuição escolhida |
| Box64 | Mantido como backend padrão |
| FEXCore ARM64EC / Wine Android | Integração experimental com pré-verificações; não validada em jogo |
| Proton Wine releases | Links oficiais e importação Wine Type TAR XZ; ABI não garantida |
| Perfis ESYNC/NTsync e HUD com intervalos de atualização | Implementados; desempenho precisa ser medido |
| APK debug | Gerado no GitHub Actions, sujeito ao resultado da branch |

## Organização

```text
winlator/
├── app/                 # submódulo upstream do aplicativo Winlator (fixado)
├── gladio/              # submódulo da pilha gráfica
├── vortek/              # submódulo da pilha gráfica
├── overrides/app/       # módulos Java, layouts e drawables do Enhanced
├── scripts/             # patcher e montagem dos pacotes Proton/FEX
├── tests/               # testes Java e Python
├── docs/                # guias, decisões técnicas, experimentos e limitações
├── .github/workflows/   # validação, APK, FEXCore e Proton/FEX
├── glibc_patches/       # recursos upstream preservados
├── installable_components/
├── wine_addons/
└── android_alsa/
```

Documentos de referência: [Central de guias](docs/README.md) • [Guia de Wine e Proton](docs/WINE_PROTON_GUIDE.md) • [Arquitetura](docs/ARCHITECTURE.md) • [Scripts](scripts/README.md) • [Roadmap](docs/ROADMAP.md).

## Desenvolver e compilar

Dependências: Git com submódulos, Python 3.10+, JDK 17 e ferramentas Android SDK/NDK do [workflow Android](.github/workflows/enhanced-android.yml).

```bash
git clone --recurse-submodules https://github.com/danilo631/winlator.git
cd winlator
git checkout feature/runtime-center-navigation-11
git submodule update --init --recursive
python3 scripts/apply_app_overrides.py --check
python3 scripts/apply_app_overrides.py
python3 -m unittest discover -s tests -p 'test_*.py' -v
cd app
./gradlew :app:assembleDebug
```

**Importante:** o patcher aplica alterações no diretório de trabalho do submódulo `app/`; ele confirma âncoras específicas do Winlator 11.2 e preserva as fontes originais versionadas no nosso repositório dentro de `overrides/`. Veja `docs/ARCHITECTURE.md` antes de modificar os submódulos.

## Atribuição e licenças

Winlator original: [Bruno Gonçalves / brunodev85](https://github.com/brunodev85/winlator). Projetos relacionados: [Wine](https://www.winehq.org/), [Box64](https://github.com/ptitSeb/box64), [FEX-Emu](https://github.com/FEX-Emu/FEX), [Valve Proton](https://github.com/ValveSoftware/Proton), [GameNative proton-wine](https://github.com/GameNative/proton-wine), [DXVK](https://github.com/doitsujin/dxvk) e [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton). Cada dependência mantém sua própria licença; consulte os repositórios originais e o [arquivo LICENSE](LICENSE) deste projeto.

Não incorporamos código ou assets proprietários do GameHub.
