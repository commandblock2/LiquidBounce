## WishList
### Some modules in scripts will be migrated to the client for better performance.

ModuleName | Category | Description | Complexity | Priority 
-----------|----------|-------------|------------|----------
VanillaAura| Combat  | The most blatant aura ever, only intended for vanilla or server that has combat disabler| Very Low | High |
ArrowDodge | Legit*   | The module for dodging arrow | Medium | Somewhat High
TPS        | Misc     | Display server's TPS | Low | Medium
WtapBot    | Legit    | The module emulate the legit player's combat action. (ad-strafe and jump tap also needed) | Somewhat Medium | Somewhat High
RodAura    | Legit    | Aura for rod| Somewhat high | Medium
ComprehensiveFightbot | Legit* | Combination of ArrowDodge / Wtapbot with other functions (including combo related like rod and placing block to start combo) | High | Low
ReachAura  | Combat  | Better ReachAura(for the ability to phase and control packet sending frequency) and much more | High | Low
Follow     | Legit*   | Automatically find the path to the selected entity | Low | Low
REPL for scripts| Misc | Better use of tab completion| Somewhat High | Medium |
InsultAssistant | Annoyance | Tab completion for the swearwords | Medium | Very High
WordTransform | Annoyance | Transform certain words on the fly to other to avoid censorship or to look l33t | Medium | Somewhat High
WordRepeater | Annoyance | Repeat some words by regex match | Low | Medium
KeywordBlock | Annoyance | Block certain chat based on regex | Low | Medium
Enemy | Annoyance | Higher priority for certain entity | Medium | Somewhat Medium



### Other than Modules  

Function  | Description | Complexity | Priority
----------|-------------|------------|----------
Various pathfinding method | For ReachAura / Follow | Somewhat High | Medium
Ownership of resource | Player's yaw/pitch, slot, pressed keybinds etc | Somewhat High | Medium
Decouple the target selection / priority from KillAura | So that aimbot / other module could have seperate instances of target selection / priority and also easier to implement enemy | Somewhat High | Medium 

### Improvements

Improvement Description | Complexity | Priority
------------------------|------------|----------
NameTag that shows enchantment | Somewhat Medium | Low
NameTag that shows entityID | Low | Medium
Better Notification(Half transparent/Acrylic) | Somewhat Medium | Medium
