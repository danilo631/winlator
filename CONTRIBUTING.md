# Contribuindo com Winlator Enhanced

O projeto preserva os submódulos Winlator e acrescenta recursos em `overrides/`. Alterações devem ser propostas por pull requests e documentadas em português ou inglês.

## Regras mínimas

1. Abra sua alteração em uma branch sobre a etapa Enhanced mais recente, **nunca edite a branch principal diretamente**.
2. Não reescreva `app/`, `vortek/` ou `gladio/` sem revisar a compatibilidade dos submódulos; as fontes Enhanced estão em `overrides/`.
3. Rode `python3 scripts/apply_app_overrides.py --check`, aplique duas vezes e garanta idempotência.
4. Rode `python3 -m unittest discover -s tests -p 'test_*.py' -v` e veja o CI Android.
5. Explique limitações de Bionic/glibc, ARM64EC, Box64, versão Wine e comportamento do Wineprefix. **Nunca anuncie FPS superiores sem benchmark repetível**.
6. Evite bibliotecas proprietárias ou componentes fechados do GameHub, credenciais ou arquivos baixados do dispositivo de terceiros.
7. Para mudanças visuais, confirme legibilidade, contraste, telas pequenas, idioma e acessibilidade.

Abra um PR em rascunho enquanto não houver teste real em Android. Siga o guia de desenvolvimento em [docs/README.md](docs/README.md).
