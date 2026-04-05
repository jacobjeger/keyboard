# MegaLife F1 Keyboard

Custom Android IME (Input Method Editor) built for the MegaLife F1 kosher Android phone. Supports both physical T9 keypad and on-screen touch keyboard input.

## Features

### Input Modes
- **T9 Multi-tap** — Physical keypad input with character cycling (press 4 twice for 'h')
- **T9 Predictive** — Type digit sequences, cycle through matching words with D-pad
- **On-screen QWERTY/Hebrew** — Full touch keyboard with swipe typing

### Languages
- **English** — QWERTY layout, full dictionary (~5000 words)
- **Hebrew** — Native Hebrew layout (RTL), dictionary (~3000 words), nikud panel, gematria display
- Switch with * key (physical) or globe button (touch)

### Word Prediction
- Dictionary-based suggestions with frequency ranking
- Learned word boosting (3x weight for words you've typed before)
- Bigram prediction (next-word suggestions based on previous word)
- Text shortcut expansion (bzh → בעזרת השם, aiy → אם ירצה השם, bh → ברוך השם)

### Autocorrect & Spell Check
- Levenshtein distance-based spelling suggestions
- Auto-correction on space/punctuation (>85% confidence threshold)
- Word blocklist — long-press a suggestion to block it

### Hebrew Features
- **Gematria** — Real-time numerical value display while typing Hebrew
- **Nikud** — Vowel mark panel (long-press period in Hebrew mode)
- **Final forms** — Automatic כ→ך מ→ם נ→ן פ→ף צ→ץ replacement at word end

### Keyboard Panels
- **Symbols** — Two pages of symbols, numbers, and special characters
- **Emoji** — Kosher-filtered emoji with category tabs, search bar, skin tone selector
- **Clipboard** — History with pin/delete, search, 1-hour auto-expiry
- **Number pad** — For number input fields
- **Phone pad** — For phone number fields

### Navigation
- **D-pad dual mode** — App navigation (D-pad controls the app) or Keyboard navigation (D-pad moves focus ring on keys)
- Toggle with long-press #

### Additional Features
- Glide/swipe typing (English)
- Voice input (Azure Speech API)
- One-handed mode (long-press space)
- Keyboard height adjustment (drag handle)
- Key preview popup on tap
- Long-press popup for alternate characters (accented letters)
- Smart punctuation (double-space → period + space)
- Auto-capitalization at sentence start
- Caps lock with 30-second reminder vibration
- Undo/redo (volume down = undo)
- Password mode (disables suggestions and learning)
- Email/URL mode detection

### Customization
- Dark / Light / Auto / High Contrast / Custom themes
- Key height (small/medium/large)
- Key border style
- Corner radius
- Text size
- Optional number row
- Haptic feedback toggle
- Key sound toggle

## Architecture

Built with Kotlin, targeting Android API 26+ (Android 8.0).

```
com.megalife.ime/
├── MegaLifeIME.kt          # InputMethodService coordinator
├── MegaLifeApplication.kt  # Application init
├── core/
│   ├── InputEngine.kt       # Central input state machine
│   ├── KeyRouter.kt         # Physical key event routing
│   ├── FeedbackManager.kt   # Haptic/sound feedback
│   ├── UndoManager.kt       # Undo/redo stack
│   └── InputConnectionHelper.kt
├── input/
│   ├── InputMode.kt         # T9/T9+/ABC/Numeric/Passthrough
│   ├── ShiftState.kt        # Lower/UpperNext/CapsLock
│   ├── MultiTapState.kt     # Key cycling state
│   └── MultiTapTimer.kt     # Coroutine-based timeout
├── language/
│   ├── LanguageConfig.kt    # Per-language configuration
│   ├── LanguageRegistry.kt  # Language switching
│   ├── T9KeyMap.kt          # Key-to-char mapping
│   ├── TouchLayout.kt       # On-screen keyboard layout definition
│   ├── SymbolLayouts.kt     # Symbol/number/phone pad layouts
│   └── locale/
│       ├── EnglishConfig.kt
│       └── HebrewConfig.kt
├── db/
│   ├── MegaLifeDatabase.kt  # Room database (5 tables)
│   ├── DictionaryLoader.kt  # CSV dictionary import
│   ├── entity/              # DictionaryWord, LearnedWord, BigramEntry, ClipboardItem, TextShortcut
│   └── dao/                 # Data access objects
├── feature/
│   ├── SuggestionManager.kt # Prediction pipeline
│   ├── SpellChecker.kt      # Autocorrect engine
│   ├── GematriaCalculator.kt
│   ├── NikudManager.kt
│   ├── EmojiData.kt         # Kosher emoji dataset
│   ├── EmojiManager.kt
│   ├── KosherEmojiFilter.kt
│   ├── ClipboardHistoryManager.kt
│   ├── TextShortcutEngine.kt
│   ├── GlideTypingController.kt
│   ├── VoiceInputManager.kt
│   ├── OneHandedController.kt
│   ├── WordBlocklist.kt
│   └── EditDistanceCalculator.kt
├── ui/
│   ├── TouchKeyboardView.kt    # Custom Canvas keyboard
│   ├── KeyboardViewManager.kt  # Panel switching
│   ├── KeyboardTheme.kt        # Theme system
│   ├── SuggestionBarView.kt
│   ├── T9StatusView.kt
│   ├── EmojiKeyboardView.kt
│   ├── ClipboardPanelView.kt
│   ├── NikudPanelView.kt
│   ├── PopupKeysView.kt
│   ├── KeyPreviewView.kt
│   └── GlideTrailView.kt
├── navigation/
│   ├── DPadNavigator.kt
│   └── KeyboardFocusManager.kt
└── settings/
    ├── IMESettingsActivity.kt
    └── PreferenceKeys.kt
```

## Building

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Installation

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell ime set com.megalife.ime/.MegaLifeIME
```

Then enable in Settings → System → Languages & Input → On-screen keyboard.

## Key Bindings (Physical Keypad)

| Key | Action |
|-----|--------|
| 2-9 | T9 character input |
| 0 | Space |
| 1 | Punctuation cycling |
| * | Switch language |
| Long * | Open settings |
| # | Cycle mode (T9 → T9+ → ABC) |
| Long # | Toggle D-pad mode |
| D-pad | Navigate (app or keyboard) |
| Volume Down | Undo |

## Privacy

- No analytics or telemetry
- No internet access except voice input (Azure Speech API)
- Clipboard items encrypted at rest and auto-expire after 1 hour
- Word learning can be disabled in settings
- All data stored locally on device
