<p align="center">
  <img src="logo.png" width="340" alt="Logotipo do Winlator" />
</p>

# Winlator Enhanced — Android

Fork experimental do [Winlator 11.2 original](https://github.com/brunodev85/winlator), dedicado a facilitar o gerenciamento de Wine, explorar Box64/FEXCore e melhorar a experiência de jogos Windows no Android. **Não é uma distribuição oficial do Winlator, Proton, Valve, FEX ou GameHub.**

> ⚠️ **Estado do projeto:** recursos desta branch ainda são experimentais e não foram validados em todos os dispositivos, especialmente GPUs Mali. Um APK compilado não comprova que jogos vão executar. Faça backup dos contêineres antes de instalar uma versão debug.

## Instalação automatizada de Wine com patches Proton

No aplicativo Enhanced, abra **☰ → Wine & Proton → Buscar e instalar Proton (GitHub)**. O app busca versões estáveis publicadas por [Kron4ek/Wine-Builds](https://github.com/Kron4ek/Wine-Builds/releases) para **Linux/glibc x86_64 WoW64**, permite escolher uma build e realiza o download em segundo plano com indicador de progresso. Confere origem, tamanho e **SHA-256 do release** antes de encaminhar automaticamente para extração, verificação e inicialização Wine do Winlator. A opção funciona sem abrir o navegador nem localizar o arquivo manualmente.

Depois de instalar, as versões aparecem em **Wine & Proton → Instalados**. Toque na versão para ver os detalhes ou removê-la, desde que nenhum contêiner a utilize. Escolha a versão em um **novo** contêiner para evitar corromper o prefixo dos contêineres existentes.

As builds [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases) são voltadas ao **Android/Bionic**, e não são instaladas automaticamente nesse rootfs glibc. O instalador automático oferece exclusivamente builds Linux/glibc com hash verificável, porém a compatibilidade com as bibliotecas do rootfs **ainda deve ser testada no aparelho**. Saiba mais em [Proton automático v4](docs/AUTO_PROTON_V4.md).

## Instalar um APK experimental pelo GitHub

O desenvolvimento validado é compilado em **`main`** através de [GitHub Actions — Winlator Enhanced / Android](https://github.com/danilo631/winlator/actions/workflows/enhanced-android.yml). Confira a execução mais recente da `main` antes de instalar.

1. Abra **Actions**, selecione o build mais recente da branch de gerenciamento Wine e verifique que **Verify overlays** e **Build Android debug APK** estejam verdes.
2. Na parte inferior da execução, vá a **Artifacts → Winlator-Enhanced-11-debug**.
3. Extraia o ZIP e instale o APK debug no Android, após guardar seus dados/contêineres. Um APK debug pode não atualizar diretamente uma versão release assinada de modo diferente.

**Instale um APK Enhanced compilado da `main`.** O menu não aparece no Winlator oficial ou em APKs antigos.

## Driver gráfico nativo para Mali, Xclipse e PowerVR

O **Vortek**, já presente no Winlator, é a ponte Vulkan para o driver do Android. Este fork adiciona seleção conservadora de Vulkan e um guardião de inicialização: em GPUs Mali/Immortalis, Xclipse e PowerVR detectadas, a execução usa Vortek + driver **System** mesmo que um atalho antigo tenha definido Turnip ou outro driver externo. Isso não altera os dados gravados no contêiner.

Em **Configurações → Acesso rápido**, o controle **Proteção automática do driver** permite desabilitar esse comportamento. GPUs desconhecidas preservam as configurações atuais. Não há root, ADB, overclock ou frequência de GPU forçada.

**Gerenciar Wine de verdade:** abra **☰ → Wine & Proton → Instalados** e toque no botão de cada versão para ver informações ou removê-la, quando nenhum contêiner estiver usando aquele Wine. Não é necessário encontrar uma seção oculta de configurações. Veja [Driver nativo e gerenciamento Wine v3](docs/NATIVE_GPU_V3.md).

## Recursos em desenvolvimento

| Área | Estado |
|---|---|
| Biblioteca de contêineres, cartões e busca | Implementada no código; requer teste no aparelho |
| Menu Wine & Proton, gerenciador direto e diagnósticos | Implementado; teste de uso no aparelho pendente |
| Instalação de Wine x86_64 compatível | Usa o instalador do Winlator; depende da distribuição escolhida |
| Box64 | Mantido como backend padrão |
| FEXCore ARM64EC / Wine Android | Integração experimental com pré-verificações; não validada em jogo |
| Proton/Wine Linux WoW64 via GitHub | Pesquisa dinâmica, download SHA-256 e importação automática; ABI ainda não garantida |
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

Documentos de referência: [Instalador automático](docs/AUTO_PROTON_V4.md) • [Central de guias](docs/README.md) • [Guia de Wine e Proton](docs/WINE_PROTON_GUIDE.md) • [Arquitetura](docs/ARCHITECTURE.md) • [Scripts](scripts/README.md) • [Roadmap](docs/ROADMAP.md).

## Desenvolver e compilar

Dependências: Git com submódulos, Python 3.10+, JDK 17 e ferramentas Android SDK/NDK do [workflow Android](.github/workflows/enhanced-android.yml).

```bash
git clone --recurse-submodules https://github.com/danilo631/winlator.git
cd winlator
git checkout main
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
