# Smooth Shading Support - Alterações Necessárias

## Objetivo
Adicionar suporte básico a smooth shading para modelos texturizados no motor, usando normais interpoladas e iluminação simples. O foco é permitir que modelos carregados em formato OBJ exibam sombreamento suave em vez de flat shading puro.

## Escopo
- suportar normais por vértice em `Model`
- carregar normais do OBJ em `ModelLoader`
- gerar normais suaves quando o OBJ não fornecer normais explícitas
- atualizar `Shader` e arquivos GLSL para calcular iluminação
- enviar matrizes e uniformes de iluminação na renderização de `TexturedModel`
- manter compatibilidade com sprites e mapas existentes

## Componentes impactados
- `java-game-engine-core/src/main/java/com/farias/rengine/render/Model.java`
- `java-game-engine-core/src/main/java/com/farias/rengine/render/Shader.java`
- `java-game-engine-core/src/main/java/com/farias/rengine/render/TexturedModel.java`
- `java-game-engine-core/src/main/java/com/farias/rengine/util/ModelLoader.java`
- `java-game-engine-core/src/main/resources/shaders/shader.vs`
- `java-game-engine-core/src/main/resources/shaders/shader.fs`
- `java-game-engine-core/src/main/java/com/farias/rengine/GameEngine.java`

## Alterações necessárias

### 1. `Model.java`
- Adicionar suporte a um buffer de normais (`n_id`) e um array `FloatBuffer normals`.
- Criar um construtor novo ou estendido que receba `float[] normals` junto com `vertices`, `tex_coords` e `indices`.
- Alterar `draw()` para habilitar e configurar o atributo de vértice de normais:
  - `glEnableVertexAttribArray(2)`
  - `glBindBuffer(GL_ARRAY_BUFFER, n_id)`
  - `glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0)`
- Manter compatibilidade com o construtor atual quando o modelo não tiver normais.
- Opcional: adicionar método para atualizar a textura e/ou normais dinamicamente.

### 2. `Shader.java`
- Registrar o atributo de normal com `glBindAttribLocation(program, 2, "normals")` antes do link.
- Atualizar os métodos `setUniform` se for necessário enviar `vec3` ou `Matrix3f`.
- Adicionar métodos de uniform extras para:
  - `lightDirection` ou `lightPosition`
  - `normalMatrix`
  - `ambientStrength`, `diffuseStrength`, `specularStrength`
  - `cameraPosition` (se houver cálculo de specular no espaço mundo/view)

### 3. `ModelLoader.java`
- Ajustar `loadWavefrontOBJ` para criar o modelo com normais:
  - preservar `float[] normalsArray` no retorno
  - passar `normalsArray` para o construtor de `TexturedModel`/`Model`
- Detectar OBJ sem normais e gerar normais suaves automaticamente.
- O algoritmo de smooth normals deve ser:
  1. Inicializar um vetor de acumulação `Vector3f[] vertexNormals` com zero.
  2. Para cada triângulo, calcular a normal da face com `cross(v2 - v1, v3 - v1)`.
  3. Adicionar a normal da face a cada vértice do triângulo.
  4. Normalizar todos os vetores de vértice.
- Garantir que `indices` e `vertices` permaneçam consistentes com `textureArray` e `normalsArray`.

### 4. `TexturedModel.java`
- Adicionar atributo `float[] normals` ou armazenar `Model` com informações de normal interna.
- Ajustar `load(...)` para receber e encaminhar normais ao criar o `Model`.
- Atualizar `draw(Matrix4f projection)` para definir uniformes adicionais, por exemplo:
  - `shader.setUniform("normalMatrix", normalMatrix)`
  - `shader.setUniform("lightDirection", lightDirection)`
  - `shader.setUniform("viewPos", cameraPosition)`
- Alternativa: mover a lógica de uniformes de iluminação para `GameEngine.drawTexturedModel` e expor um método `draw(Matrix4f projection, Matrix4f normalMatrix)`.

### 5. `GameEngine.java`
- Modificar `drawTexturedModel(TexturedModel texturedModel)` para calcular e enviar a normal matrix:
  - `Matrix4f modelViewMatrix = camera.getViewMatrix().mul(modelMatrix, new Matrix4f());`
  - `Matrix4f normalMatrix = new Matrix4f(modelViewMatrix).invert().transpose();`
  - `Matrix4f projection = projection_matrix.mul(modelViewMatrix, new Matrix4f());`
- Caso prefira manter `TexturedModel.draw(projection)`, adotar uma nova assinatura que aceite `normalMatrix`.
- Enviar uniformes globais de iluminação se o shader usar valores compartilhados.

### 6. Arquivos GLSL (`shader.vs` e `shader.fs`)
- `shader.vs`:
  - adicionar atributo `attribute vec3 normals;`
  - adicionar `varying vec3 fragNormal;`
  - adicionar `uniform mat3 normalMatrix;`
  - calcular `fragNormal = normalize(normalMatrix * normals);`
- `shader.fs`:
  - receber `varying vec3 fragNormal;`
  - definir uniform `uniform vec3 lightDirection;`
  - calcular iluminação ambient + diffuse básica:
    - `float diff = max(dot(normalize(fragNormal), normalize(lightDirection)), 0.0);`
    - `vec3 color = ambient + diff * diffuse;`
  - multiplcar o resultado pelo `texture2D(sampler, tex_coords)`.
- Opcional: adicionar specular simples usando `reflect()` e `viewDir`.

### 7. Propriedades de material
- Definir valores padrão simples para materiais de `TexturedModel`:
  - `ambientFactor = 0.2`
  - `diffuseFactor = 0.8`
  - `specularFactor = 0.2`
  - `shininess = 16`
- Essas propriedades podem ficar em `TexturedModel` ou em `Shader` como constantes.
- Se preferir, comece apenas com `ambient` e `diffuse` para manter a implementação leve.

### 8. Compatibilidade com sprites/mapas
- Não é necessário alterar `Sprite` ou `Map` se eles continuarem usando o shader atual sem normais.
- Garantir que `glBindAttribLocation` ainda atribua `vertices` e `textures` corretamente para esses casos.
- Para futuros usos 3D com mapas, criar shader separado em vez de forçar mudanças no shader atual.

## Detalhes de implementação sugeridos

### Novo construtor `Model`
```java
public Model(float[] vertices, float[] tex_coords, float[] normals, int[] indices) {
    // cria buffers de vértice, textura, normal e indices
}
```

### Suporte fallback para objetos sem normais
- Se `normals == null`, gerar normais suaves internamente em `ModelLoader`.
- Se `normalsArray` estiver vazio, usar o algoritmo de normal smoothing antes de criar o `TexturedModel`.

### Uniformes e atributos mínimos
A shader precisa receber pelo menos:
- `attribute vec3 vertices;`
- `attribute vec2 textures;`
- `attribute vec3 normals;`
- `uniform mat4 projection;`
- `uniform mat3 normalMatrix;`
- `uniform vec3 lightDirection;`
- `uniform sampler2D sampler;`

## Passos de testes

1. Carregar um modelo OBJ com normais fornecidas e verificar se os triângulos exibem smooth shading.
2. Carregar um modelo OBJ sem normais e verificar se o algoritmo de geração de normais produz sombreamento suave em vez de faces planas.
3. Comparar renderização com a textura ativa para garantir que o `sampler` continua funcionando.
4. Verificar que sprites/mapas continuam desenhando sem alterações.

## Resultado esperado
- modelos 3D texturizados apresentam sombreamento suave,
- a iluminação básica é calculada no fragment shader,
- os atributos existentes (`vertices`, `textures`) permanecem compatíveis,
- a nova normal matrix permite preservar iluminação correta sob transformação.

## Prioridade das alterações
1. Atualizar `Model.java` para aceitar normais
2. Atualizar `Shader.java` para vincular atributo de normals
3. Atualizar `ModelLoader.java` para carregar e/ou gerar normais
4. Atualizar `shader.vs` e `shader.fs` para lighting
5. Atualizar `TexturedModel.java` e `GameEngine.drawTexturedModel`
6. Testar com modelos OBJ reais e fallback sem normais
