# Design System - Server-Driven UI

Um Design System moderno e flexível para Android que permite renderizar interfaces de usuário dinamicamente a partir de definições JSON do Firebase Remote Config.

## 📋 Índice

- [Características](#-características)
- [Instalação](#-instalação)
- [Configuração](#-configuração)
- [Componentes Disponíveis](#-componentes-disponíveis)
- [Validação de Formulários](#-validação-de-formulários)
- [Sistema de Eventos](#-sistema-de-eventos)
- [Exemplos de Uso](#-exemplos-de-uso)
- [Arquitetura](#-arquitetura)
- [Acessibilidade](#-acessibilidade)

## ✨ Características

- 🎨 **Server-Driven UI**: Renderize telas completas a partir de JSON
- ✅ **Validação Integrada**: Sistema completo de validação de formulários (CPF, email, regex, etc.)
- 🔄 **Event Stream Reativo**: Sistema de eventos baseado em Kotlin Flow
- ♿ **Acessibilidade**: Suporte completo para TalkBack e navegação por acessibilidade
- 🎯 **Type-Safe**: Interface Kotlin totalmente tipada
- 🔌 **Modular**: Arquitetura limpa e extensível
- 🔥 **Firebase Integration**: Integração nativa com Firebase Remote Config e Analytics
- 💉 **Hilt/Dagger**: Injeção de dependências configurada

## 📦 Instalação

### Gradle

Adicione o repositório no seu arquivo `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Adicione a dependência no seu `build.gradle.kts` do módulo app:

```kotlin
dependencies {
    implementation("com.github.domleondev:designsystem:1.0.0")
    
    // Dependências necessárias
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-config")
}
```

## 🔧 Configuração

### 1. Configure o Hilt na sua Application

```kotlin
@HiltAndroidApp
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
```

### 2. Injete as dependências na Activity

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var uiRenderer: UiRenderer
    
    @Inject
    lateinit var designSystem: DesignSystem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure seu layout
        observeDesignSystemEvents()
    }
}
```

### 3. Configure o Firebase Remote Config

Adicione a definição JSON da sua tela no Firebase Remote Config:

```json
{
  "screen": "login_screen",
  "components": [
    {
      "component": "Header",
      "props": {
        "title": "Login",
        "showBack": true
      }
    },
    {
      "component": "Input",
      "id": "email",
      "props": {
        "hint": "E-mail",
        "keyboardType": "EMAIL",
        "validateOnChange": true,
        "rules": [
          {
            "component": "required",
            "message": "E-mail é obrigatório"
          },
          {
            "component": "email",
            "message": "E-mail inválido"
          }
        ]
      }
    },
    {
      "component": "Button",
      "id": "submit_btn",
      "props": {
        "text": "Entrar",
        "submit": true,
        "backgroundColor": "#0056D2",
        "textColor": "#FFFFFF"
      }
    }
  ]
}
```

## 🧩 Componentes Disponíveis

### Header
Header customizável com ícone de voltar ou menu.

```json
{
  "component": "Header",
  "props": {
    "title": "Título da Tela",
    "showBack": true,
    "showMenu": false,
    "titleSize": 20,
    "textColor": "#000000",
    "typeface": "roboto_bold"
  }
}
```

### Input
Campo de entrada com validação integrada.

**Tipos de Teclado**: `TEXT`, `EMAIL`, `PASSWORD`, `CPF`, `NUMBER`, `PHONE`

```json
{
  "component": "Input",
  "id": "email_input",
  "props": {
    "hint": "Digite seu e-mail",
    "keyboardType": "EMAIL",
    "height": 56,
    "validateOnChange": true,
    "rules": [...]
  }
}
```

### Button
Botão customizável com suporte a submit de formulários.

```json
{
  "component": "Button",
  "id": "login_btn",
  "props": {
    "text": "Entrar",
    "submit": true,
    "backgroundColor": "#0056D2",
    "textColor": "#FFFFFF",
    "enabled": true
  }
}
```

### Text
Texto customizável com suporte a spans coloridos.

```json
{
  "component": "Text",
  "props": {
    "title": "Bem-vindo",
    "size": 24,
    "weight": "bold",
    "textColor": "#000000",
    "alignment": "center",
    "spans": [
      { "text": "Olá, ", "color": "#000000" },
      { "text": "Usuário", "color": "#0056D2" }
    ]
  }
}
```

### MenuItem
Item de menu lateral.

```json
{
  "component": "MenuItem",
  "id": "profile",
  "props": {
    "text": "Perfil",
    "icon": "ic_person",
    "iconColor": "#424242",
    "textColor": "#000000",
    "action": "navigate:profile"
  }
}
```

### NewsCard
Card de notícia com imagem e texto.

```json
{
  "component": "NewsCard",
  "props": {
    "imageUrl": "https://example.com/image.jpg",
    "title": "Título da Notícia",
    "description": "Descrição da notícia..."
  }
}
```

### SelectableChip
Chip selecionável para filtros.

```json
{
  "component": "SelectableChip",
  "id": "chip_1",
  "props": {
    "text": "Tecnologia",
    "selected": false
  }
}
```

### Containers

#### VerticalContainer
```json
{
  "component": "VerticalContainer",
  "children": [...]
}
```

#### HorizontalContainer
```json
{
  "component": "HorizontalContainer",
  "children": [...]
}
```

#### FlowContainer
Container de chips com wrap automático.

```json
{
  "component": "FlowContainer",
  "children": [
    { "component": "SelectableChip", ... }
  ]
}
```

## ✅ Validação de Formulários

O Design System possui um sistema robusto de validação de formulários com suporte a múltiplas regras.

### Regras de Validação Disponíveis

#### Required
```json
{
  "component": "required",
  "message": "Campo obrigatório"
}
```

#### Email
```json
{
  "component": "email",
  "message": "E-mail inválido"
}
```

#### MinLength
```json
{
  "component": "minlength",
  "params": { "min": 8 },
  "message": "Mínimo de 8 caracteres"
}
```

#### CPF
```json
{
  "component": "cpf",
  "message": "CPF inválido"
}
```

#### Match
Valida se dois campos têm o mesmo valor (útil para confirmação de senha).

```json
{
  "component": "match",
  "params": { "targetId": "password" },
  "message": "As senhas não coincidem"
}
```

#### Regex
```json
{
  "component": "regex",
  "params": { "pattern": "^[A-Z].*" },
  "message": "Deve começar com letra maiúscula"
}
```

### Validação Programática

```kotlin
// Validar todos os campos
when (val result = designSystem.validate()) {
    is DsValidationResult.Valid -> {
        // Formulário válido
    }
    is DsValidationResult.Invalid -> {
        // result.errors contém os erros por campo
        result.errors.forEach { (fieldId, errorMessage) ->
            Log.e("Validation", "$fieldId: $errorMessage")
        }
    }
}

// Validar campos específicos
val result = designSystem.validate("email", "password")

// Limpar validação
designSystem.clearValidation()
designSystem.clearValidation("email") // Específico

// Definir erro manualmente
designSystem.setError("email", "Este e-mail já está cadastrado")

// Obter valor de um campo
val email = designSystem.getValue("email")
```

## 📡 Sistema de Eventos

O Design System emite eventos através de um Flow reativo.

### Tipos de Eventos

```kotlin
sealed class DsUiEvent {
    data class Click(val componentId: String)
    data class Change(val componentId: String, val value: String)
    data class Submit(val screenId: String)
    data class Analytics(val eventName: String)
    data class Action(val componentId: String, val action: String)
}
```

### Observando Eventos

```kotlin
lifecycleScope.launch {
    designSystem.eventStream().events.collect { event ->
        when (event) {
            is DsUiEvent.Submit -> {
                // Formulário submetido
                handleSubmit()
            }
            is DsUiEvent.Change -> {
                // Campo alterado
                Log.d("Input", "${event.componentId}: ${event.value}")
            }
            is DsUiEvent.Action -> {
                // Ação customizada
                handleNavigation(event.action)
            }
            is DsUiEvent.Analytics -> {
                // Evento de analytics
                firebaseAnalytics.logEvent(event.eventName, null)
            }
            else -> {}
        }
    }
}
```

## 📝 Exemplos de Uso

### Renderizando uma Tela do Remote Config

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val renderScreenUseCase: RenderScreenUseCase,
    private val repository: RemoteConfigRepository
) : ViewModel() {
    
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun loadScreen(key: String) {
        _uiState.value = UiState.Loading
        repository.fetchScreenConfig(key) { json ->
            val screenDefinition = renderScreenUseCase(json)
            if (screenDefinition != null) {
                _uiState.value = UiState.Success(screenDefinition)
            } else {
                _uiState.value = UiState.Error("Erro ao processar tela")
            }
        }
    }
}
```

```kotlin
viewModel.uiState.observe(this) { state ->
    when (state) {
        is UiState.Loading -> {
            progressBar.visibility = View.VISIBLE
        }
        is UiState.Success -> {
            progressBar.visibility = View.GONE
            uiRenderer.render(containerLayout, state.screen)
        }
        is UiState.Error -> {
            progressBar.visibility = View.GONE
            Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
        }
    }
}
```

### Formulário de Login Completo

```json
{
  "screen": "login_screen",
  "components": [
    {
      "component": "Header",
      "props": {
        "title": "Entrar",
        "showBack": true
      }
    },
    {
      "component": "Text",
      "props": {
        "title": "Bem-vindo de volta!",
        "size": 24,
        "weight": "bold",
        "margin_top": 32,
        "margin_bottom": 8
      }
    },
    {
      "component": "Text",
      "props": {
        "title": "Faça login para continuar",
        "size": 14,
        "textColor": "#666666",
        "margin_bottom": 32
      }
    },
    {
      "component": "Input",
      "id": "email",
      "props": {
        "hint": "E-mail",
        "keyboardType": "EMAIL",
        "validateOnChange": true,
        "margin_bottom": 16,
        "rules": [
          {
            "component": "required",
            "message": "E-mail é obrigatório"
          },
          {
            "component": "email",
            "message": "E-mail inválido"
          }
        ]
      }
    },
    {
      "component": "Input",
      "id": "password",
      "props": {
        "hint": "Senha",
        "keyboardType": "PASSWORD",
        "validateOnChange": true,
        "margin_bottom": 24,
        "rules": [
          {
            "component": "required",
            "message": "Senha é obrigatória"
          },
          {
            "component": "minlength",
            "params": { "min": 6 },
            "message": "Senha deve ter no mínimo 6 caracteres"
          }
        ]
      }
    },
    {
      "component": "Button",
      "id": "login_btn",
      "props": {
        "text": "Entrar",
        "submit": true,
        "backgroundColor": "#0056D2",
        "textColor": "#FFFFFF"
      }
    }
  ]
}
```

### Tela com Filtros (Chips)

```json
{
  "screen": "filters_screen",
  "components": [
    {
      "component": "Header",
      "props": {
        "title": "Filtros",
        "showBack": true
      }
    },
    {
      "component": "Text",
      "props": {
        "title": "Selecione as categorias",
        "size": 16,
        "weight": "bold",
        "margin_top": 24,
        "margin_bottom": 16
      }
    },
    {
      "component": "FlowContainer",
      "children": [
        {
          "component": "SelectableChip",
          "id": "tech",
          "props": {
            "text": "Tecnologia",
            "selected": false
          }
        },
        {
          "component": "SelectableChip",
          "id": "sports",
          "props": {
            "text": "Esportes",
            "selected": false
          }
        },
        {
          "component": "SelectableChip",
          "id": "business",
          "props": {
            "text": "Negócios",
            "selected": true
          }
        }
      ]
    },
    {
      "component": "Button",
      "id": "apply_btn",
      "props": {
        "text": "Aplicar Filtros",
        "backgroundColor": "#0056D2",
        "textColor": "#FFFFFF",
        "margin_top": 32
      }
    }
  ]
}
```

## 🏗️ Arquitetura

O módulo segue os princípios da Clean Architecture e SOLID:

```
designsystem/
├── contract/           # Interfaces públicas
│   ├── DesignSystem
│   ├── DsEventStream
│   └── DsUiEvent
├── domain/
│   ├── model/         # Modelos de domínio
│   ├── renderer/      # Interface UiRenderer
│   ├── repository/    # Interfaces de repositórios
│   └── usecase/       # Casos de uso
├── data/
│   └── repository/    # Implementações de repositórios
├── presentation/
│   ├── renderer/      # BackendDrivenUiRenderer
│   └── state/         # Estados de UI
├── runtime/           # DesignSystemImpl (core)
├── parser/            # Parser JSON
└── di/               # Módulos Hilt
```

### Componentes Principais

- **DesignSystem**: Interface principal para interagir com o sistema
- **UiRenderer**: Responsável por renderizar componentes na tela
- **ComponentFactory**: Factory para criação de Views Android
- **JsonParser**: Parser de definições JSON para models
- **RemoteConfigRepository**: Busca configurações do Firebase

## ♿ Acessibilidade

O Design System possui suporte completo para acessibilidade:

### Propriedades de Acessibilidade

```json
{
  "component": "Button",
  "props": {
    "text": "Enviar",
    "accessibilityLabel": "Botão de enviar formulário",
    "accessibilityHint": "Toque duas vezes para enviar",
    "importantForAccessibility": "yes",
    "isHeading": false
  }
}
```

### Navegação por Foco

```json
{
  "component": "Input",
  "props": {
    "hint": "Nome",
    "next_focus_down": "@id/email_input",
    "next_focus_up": "@id/previous_input"
  }
}
```

### Recursos de Acessibilidade

- ✅ Suporte completo ao TalkBack
- ✅ Navegação por teclado/D-pad
- ✅ Anúncios de erros de validação
- ✅ Estados descritivos para leitores de tela
- ✅ Live regions para atualizações dinâmicas
- ✅ Headings semânticos

## 🎨 Propriedades Visuais Comuns

Todas as views suportam as seguintes propriedades:

### Margens
```json
{
  "margin_top": 16,
  "margin_bottom": 16,
  "margin_left": 16,
  "margin_right": 16
}
```

### Estilo Visual
```json
{
  "backgroundColor": "#FFFFFF",
  "textColor": "#000000",
  "border_radius": 8,
  "border_color": "#CCCCCC",
  "visibility": "visible",
  "enabled": true
}
```

### Layout
```json
{
  "alignment": "center",  // "left", "right", "center"
  "flex": 1.0
}
```

## 🔄 Gerenciamento de Estado

### Habilitando/Desabilitando Componentes

```kotlin
designSystem.setEnabled("submit_btn", true)
```

### Obtendo Valores

```kotlin
val email = designSystem.getValue("email")
val password = designSystem.getValue("password")
```

### Limpando o State

```kotlin
// Limpa todos os registros e listeners
designSystem.clear()
```

**Importante**: O método `clear()` é chamado automaticamente pelo `UiRenderer` ao renderizar uma nova tela, garantindo que não haja conflitos entre navegações.

## 🎯 Sistema de Ações

### Ações de Navegação

```json
{
  "component": "Button",
  "props": {
    "text": "Voltar",
    "action": "navigate:back"
  }
}
```

Padrões suportados:
- `navigate:back` - Volta para a tela anterior
- `navigate:login` - Navega para login
- `navigate:{destination}` - Navegação customizada
- `menu:open` - Abre menu lateral
- Qualquer ação customizada

### Tratamento de Ações

```kotlin
lifecycleScope.launch {
    designSystem.eventStream().events.collect { event ->
        when (event) {
            is DsUiEvent.Action -> {
                when {
                    event.action.startsWith("navigate:") -> {
                        val destination = event.action.substringAfter(":")
                        navigateTo(destination)
                    }
                    event.action == "menu:open" -> {
                        showSideMenu()
                    }
                }
            }
        }
    }
}
```

## 📊 Analytics

Eventos de analytics podem ser definidos no JSON:

```json
{
  "component": "Button",
  "props": {
    "text": "Comprar Agora",
    "analytics": "purchase_button_clicked"
  }
}
```

E coletados no app:

```kotlin
is DsUiEvent.Analytics -> {
    val bundle = Bundle().apply {
        putString("origin", "server_driven_ui")
    }
    firebaseAnalytics.logEvent(event.eventName, bundle)
}
```

## 🛠️ API Pública

### Interface DesignSystem

```kotlin
interface DesignSystem {
    fun createView(context: Context, component: Component): View?
    fun validate(vararg fieldIds: String): DsValidationResult
    fun eventStream(): DsEventStream
    fun clearValidation(vararg fieldIds: String)
    fun setEnabled(id: String, enabled: Boolean)
    fun getValue(id: String): String?
    fun setError(id: String, message: String?)
    fun clear()
}
```

### Interface UiRenderer

```kotlin
interface UiRenderer {
    fun render(container: ViewGroup, screen: ScreenDefinition)
}
```

## 🧪 Testando

### Unit Tests

```kotlin
@Test
fun `should validate email correctly`() {
    val result = designSystem.validate("email")
    assertTrue(result is DsValidationResult.Valid)
}
```

### UI Tests

```kotlin
@Test
fun `should render screen from json`() {
    val json = """{ "screen": "test", "components": [...] }"""
    val screen = renderScreenUseCase(json)
    assertNotNull(screen)
}
```

## 🚀 Performance

- ✅ Views são reutilizadas quando possível
- ✅ Listeners são removidos adequadamente para evitar memory leaks
- ✅ Registros internos são limpos automaticamente entre navegações
- ✅ Lazy loading de imagens com Coil
- ✅ Event flow com buffer otimizado

## 👥 Autores

- **Squad 03** 

---

⭐ Se este projeto foi útil, considere dar uma estrela no GitHub!

 
