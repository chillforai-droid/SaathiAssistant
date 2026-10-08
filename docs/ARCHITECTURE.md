# Architecture

Single-module Android app, MVVM, Kotlin coroutines + StateFlow, Jetpack Compose UI, manual DI (`AppContainer`). No backend, no network permission.

```
com.saathi.assistant
├─ command/        Command, CommandType, ParsedCommand, ActionPlan, CommandParser (pure, JVM-testable),
│                  CommandInterpreter (+ RuleBasedCommandInterpreter), SettingsKeys
├─ automation/     Action/ActionType model, ActionValidator (pure), ActionEngine, AppLauncher, UserInteraction
├─ safety/         SafetyClassifier (SAFE / CONFIRM / BLOCK)
├─ accessibility/  SaathiAccessibilityService, AccessibilityStateManager, AccessibilityNodeFinder, AccessibilityActionExecutor
├─ adapters/       AppAdapter, GenericAdapter, Messaging/Profile adapters, AdapterRegistry, KnownApps catalog
├─ workflow/       Workflow model, WorkflowJson (export/import/validate), WorkflowRepository (local JSON file)
├─ data/           HistoryRepository, SettingsStore, ContactResolver
├─ permissions/    PermissionHelper
└─ ui/             MainViewModel, SpeechController, navigation, screens
```

## Flow
`text/voice → CommandParser → ParsedCommand → CommandInterpreter → ActionPlan → ActionEngine → AppAdapter → (Intent | AccessibilityActionExecutor)`

1. **CommandParser** is rule-based Hindi/English/Hinglish (token normalisation, app alias table, verb vocab). It only *describes* intent.
2. **CommandInterpreter** turns the intent into an `ActionPlan` (list of `Action`). Guided flows (send message) come from the app's adapter.
3. **ActionEngine** per step: validate → `SafetyClassifier` (BLOCK stops, CONFIRM asks via `UserInteraction`) → delay → run with timeout + retry → log. A failed step stops the run; success is never faked. Cancel = cancel the coroutine. Workflows can nest (depth ≤ 3).
4. **Adapters** hold per-app knowledge; `GenericAdapter` implements all core actions so new apps need no engine changes.

## Optional AI (future)
`CommandInterpreter` is the seam. A future `AiCommandInterpreter` can produce the same `ActionPlan`; it must go through the same validator, classifier and confirmations. Nothing in v1 depends on it; never put API keys in source.

## Workflow JSON
`Workflow{id,name,description,triggerPhrases[],actions[],requiresConfirmation,enabled,favorite,createdAt,updatedAt}` and `Action{id,type,target,value,delayMs,confirmationRequired,enabled}`. Import validates size (256 KB), JSON, every action (`ActionValidator`) and rejects BLOCKED steps; ids are regenerated.

## Roadmap hooks
Visual recorder (new UI feeding `Action`s), scheduled workflows (WorkManager + same engine), notification triggers, better Hindi parsing (extend vocab in `CommandParser`), optional AI interpreter.
