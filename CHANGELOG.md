# Changelog

## [Não lançado]

### Adicionado
- `Input.getScrollY()`: rolagem vertical da roda do mouse no frame, implementada no `GlfwInput`.
- `IsometricCamera.rotate(degrees)` e `yaw()`: a câmera orbita em volta do eixo Y do alvo;
  `pan`, `viewMatrix` e `directionToEye` acompanham o yaw.
- `LitMaterial.viewDirection(...)`: atualiza só a direção da câmera do brilho especular, para
  câmeras que se movem.
- `LitMaterial.tint(color, strength)`: mistura uma cor sobre a superfície (destaque de hover ou
  seleção), com os uniforms `tintColor` e `tintStrength` no shader padrão.

### Corrigido
- `GlfwWindow.destroy()` libera os callbacks registrados na janela antes de destruí-la.
