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

### Demos
- `IsometricChess`: contorno da peça sob o cursor, zoom pela roda e rotação arrastando com o botão
  do meio; peças em `examples.chess.model.pieces` (uma subclasse por tipo) com os movimentos válidos de
  cada uma, e o caminho até o destino sob o cursor destacado com tint.
- `IsometricChess`: clicar num destino válido move a peça; a peça capturada é levada para fora do
  tabuleiro, do lado de quem capturou, com animação (sobe, atravessa, desce). Regras
  (`ChessGame`, `ChessBoard.move`) separadas da interação (`ChessInteraction`).

### Corrigido
- `GlfwWindow.destroy()` libera os callbacks registrados na janela antes de destruí-la.
