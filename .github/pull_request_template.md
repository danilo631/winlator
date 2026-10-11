## Mudanças

Descreva o que mudou, que jogo/runtime é afetado e por que a alteração é necessária.

## Compatibilidade

- [ ] Box64 / Wine padrão continua funcional
- [ ] Se envolver Proton/FEX: arquitetura ABI e bibliotecas identificadas
- [ ] Não copia componentes ou assets proprietários
- [ ] Não modifica um prefixo Wine existente sem backup/migração

## Validação

- [ ] `python3 scripts/apply_app_overrides.py --check`
- [ ] Overlays aplicados duas vezes sem efeitos colaterais
- [ ] Testes Java/Python no GitHub Actions
- [ ] APK debug gerado pelo GitHub Actions
- [ ] Navegação no Android físico verificada (quando envolver interface)
- [ ] FPS/frametimes comparados com protocolo equivalente (quando alegar desempenho)

## Riscos e documentação

Informe limitações, screenshots quando aplicável, comportamento sem Wine instalado e referências da documentação.
